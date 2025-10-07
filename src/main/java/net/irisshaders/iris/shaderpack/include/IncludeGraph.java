package net.irisshaders.iris.shaderpack.include;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.error.RusticError;
import net.irisshaders.iris.shaderpack.transform.line.LineTransform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public final class IncludeGraph {
	private static final IncludeGraph EMPTY = new IncludeGraph(
			ImmutableMap.of(), ImmutableMap.of(), new int[0], new AbsolutePackPath[0], new int[0]);
	private final ImmutableMap<AbsolutePackPath, FileNode> nodes;
	private final ImmutableMap<AbsolutePackPath, RusticError> failures;
	private final int[] adjacency;
	private final AbsolutePackPath[] vertex;
	private final int[] offset;

	public IncludeGraph(Path root, ImmutableList<AbsolutePackPath> starts) {
		if (starts.isEmpty()) {
			this.nodes = ImmutableMap.of();
			this.failures = ImmutableMap.of();
			this.adjacency = new int[0];
			this.vertex = new AbsolutePackPath[0];
			this.offset = new int[0];
			return;
		}
		Builder b = new Builder(root, starts);
		this.nodes = ImmutableMap.copyOf(b.nodes);
		this.failures = ImmutableMap.copyOf(b.failures);
		this.adjacency = b.adjacency.toIntArray();
		this.vertex = b.vertex.toArray(new AbsolutePackPath[0]);
		this.offset = buildOffset(adjacency, vertex.length);
	}

	public ImmutableMap<AbsolutePackPath, FileNode> getNodes() { return nodes; }
	public ImmutableMap<AbsolutePackPath, RusticError> getFailures() { return failures; }

	public List<IncludeGraph> computeWeaklyConnectedComponents() {
		int n = vertex.length;
		if (n == 0) return List.of(EMPTY);
		UnionFind uf = new UnionFind(n);
		for (int u = 0; u < n; u++) {
			int b = offset[u], e = offset[u + 1];
			for (int i = b; i < e; i++) uf.union(u, adjacency[i]);
		}
		Int2ObjectMap<IncludeGraph> components = new Int2ObjectOpenHashMap<>();
		for (int u = 0; u < n; u++) {
			int root = uf.find(u);
			if (!components.containsKey(root)) {
				components.put(root, extractComponent(root, uf));
			}
		}
		return List.copyOf(components.values());
	}

	public IncludeGraph map(Function<AbsolutePackPath, LineTransform> transformProvider) {
		ImmutableMap.Builder<AbsolutePackPath, FileNode> b = ImmutableMap.builder();
		nodes.forEach((p, n) -> b.put(p, n.map(transformProvider.apply(p))));
		return new IncludeGraph(b.build(), failures, adjacency, vertex, offset);
	}

	private static final class UnionFind {
		private final int[] parent;
		UnionFind(int n) {
			parent = new int[n];
			for (int i = 0; i < n; i++) parent[i] = i;
		}
		int find(int x) { return parent[x] == x ? x : (parent[x] = find(parent[x])); }
		void union(int x, int y) { parent[find(x)] = find(y); }
	}

	private IncludeGraph(ImmutableMap<AbsolutePackPath, FileNode> nodes,
						 ImmutableMap<AbsolutePackPath, RusticError> failures,
						 int[] adjacency,
						 AbsolutePackPath[] vertex,
						 int[] offset) {
		this.nodes = nodes;
		this.failures = failures;
		this.adjacency = adjacency;
		this.vertex = vertex;
		this.offset = offset;
	}

	private static final class Builder {
		final Path root;
		final Object2ObjectMap<AbsolutePackPath, FileNode> nodes = new Object2ObjectOpenHashMap<>();
		final Object2ObjectMap<AbsolutePackPath, RusticError> failures = new Object2ObjectOpenHashMap<>();
		final IntArrayList adjacency = new IntArrayList();
		final ObjectArrayList<AbsolutePackPath> vertex = new ObjectArrayList<>();
		final Object2IntMap<AbsolutePackPath> idOf = new Object2IntOpenHashMap<>(256, 0.75f);
		final ObjectArrayList<IntArrayList> edges;

		Builder(Path root, ImmutableList<AbsolutePackPath> starts) {
			this.root = root;
			idOf.defaultReturnValue(-1);
			int est = Math.min(starts.size() * 4, 256);
			edges = new ObjectArrayList<>(est);
			edges.size(est);
			Deque<AbsolutePackPath> queue = new ArrayDeque<>(starts);
			Set<AbsolutePackPath> seen = new HashSet<>(est);
			for (AbsolutePackPath s : starts) {
				if (seen.add(s)) queue.addLast(s);
			}
			while (!queue.isEmpty()) {
				AbsolutePackPath cur = queue.removeFirst();
				int u = vertexId(cur);
				FileNode node = loadNode(cur);
				if (node == null) continue;
				for (var inc : node.getIncludes().entrySet()) {
					int line = inc.getKey();
					AbsolutePackPath target = inc.getValue();
					if (Objects.equals(cur, target)) {
						recordFailure(cur, line);
						continue;
					}
					int v = vertexId(target);
					edges.get(u).add(v);
					if (seen.add(target)) queue.addLast(target);
				}
			}
			compact();
			detectCycle();
		}

		private int vertexId(AbsolutePackPath p) {
			int id = idOf.getInt(p);
			if (id != -1) return id;
			id = vertex.size();
			vertex.add(p);
			idOf.put(p, id);
			if (id >= edges.size()) edges.size(id * 2);
			if (edges.get(id) == null) edges.set(id, new IntArrayList(4));
			return id;
		}

		private FileNode loadNode(AbsolutePackPath p) {
			if (nodes.containsKey(p)) return nodes.get(p);
			try {
				String src = Files.readString(p.resolved(root));
				FileNode node = new FileNode(p, ImmutableList.copyOf(src.split("\\R", -1)));
				nodes.put(p, node);
				return node;
			} catch (IOException e) {
				String top = (e instanceof NoSuchFileException) ? "file not found" : "I/O error";
                RusticError err = new RusticError("error", "failed to resolve #include", top,
						p.getPathString(), -1, null);
				failures.put(p, err);
				return null;
			}
		}

		private void recordFailure(AbsolutePackPath p, int line) {
			failures.put(p, new RusticError("error", "trivial #include cycle", "file includes itself", p.getPathString(), line + 1,
					nodes.get(p).getLines().get(line)));
		}

		private void compact() {
			int n = vertex.size();
			for (int u = 0; u < n; u++) {
				IntArrayList lst = edges.get(u);
				if (lst == null || lst.isEmpty()) continue;
				adjacency.addElements(adjacency.size(), lst.elements(), 0, lst.size());
			}
		}

		private void detectCycle() {
			int n = vertex.size();
			if (n == 0) return;
			IntSet onStack = new IntOpenHashSet(n);
			IntArrayList stack = new IntArrayList(n);
			Int2IntMap parent = new Int2IntOpenHashMap(n);
			parent.defaultReturnValue(-1);
			IntArrayList path = new IntArrayList(n);
			for (int start = 0; start < n; start++) {
				if (parent.containsKey(start)) continue;
				stack.push(start);
				parent.put(start, -1);
				while (!stack.isEmpty()) {
					int u = stack.pop();
					if (onStack.contains(u)) {
						extractCycle(u, parent, path);
						return;
					}
					onStack.add(u);
					int b = offset(u), e = offset(u + 1);
					for (int i = b; i < e; i++) {
						int v = adjacency.getInt(i);
						if (!parent.containsKey(v)) {
							parent.put(v, u);
							stack.push(v);
						}
					}
				}
				onStack.clear();
			}
		}

		private void extractCycle(int meet, Int2IntMap parent, IntArrayList path) {
			for (int u = meet; u != -1; u = parent.get(u)) path.add(u);
			Collections.reverse(path);
			StringBuilder err = new StringBuilder();
			for (int i = 0, n = path.size(); i < n; i++) {
				int u = path.getInt(i);
				AbsolutePackPath p = vertex.get(u);
				FileNode node = nodes.get(p);
				int line = -1;
				String badLine = "";
				if (i > 0) {
					int prev = path.getInt(i - 1);
					for (var e : nodes.get(vertex.get(prev)).getIncludes().entrySet()) {
						if (e.getValue().equals(p)) {
							line = e.getKey() + 1;
							badLine = node.getLines().get(line - 1);
							break;
						}
					}
				}
				String kind = (i == 0) ? "error" : "note";
				String msg = (i == 0) ? "#include cycle detected" : "cycle involves another file";
				err.append(new RusticError(kind, msg, (i == 0) ? "cycle" : "continues",
						p.getPathString(), line, badLine)).append('\n');
			}
			err.append("note: #include guards will not work\n");
			Iris.logger.error(err.toString());
			throw new IllegalStateException("Cycle in #include graph, see log");
		}

		private int offset(int u) {
			int off = 0;
			for (int i = 0; i < u && i < vertex.size(); i++)
				off += degree(i);
			return off;
		}

		private int degree(int u) {
			IntArrayList lst = edges.get(u);
			return (lst == null) ? 0 : lst.size();
		}
	}

	private IncludeGraph extractComponent(int root, UnionFind uf) {
		ImmutableMap.Builder<AbsolutePackPath, FileNode> subNodes = ImmutableMap.builder();
		ImmutableMap.Builder<AbsolutePackPath, RusticError> subFails = ImmutableMap.builder();
		for (int u = 0; u < vertex.length; u++) {
			if (uf.find(u) == root) {
				AbsolutePackPath p = vertex[u];
				subNodes.put(p, Objects.requireNonNull(nodes.get(p)));
				if (failures.containsKey(p)) subFails.put(p, Objects.requireNonNull(failures.get(p)));
			}
		}
		return new IncludeGraph(subNodes.build(), subFails.build(), new int[0], new AbsolutePackPath[0], new int[0]);
	}

	private static int[] buildOffset(int[] adj, int n) {
		int[] off = new int[n + 1];
		int pos = 0;
		for (int u = 0; u < n; u++) {
			int end = (u + 1 < n ? findNextOffset(adj) : adj.length);
			off[u] = pos;
			pos = end;
		}
		off[n] = adj.length;
		return off;
	}

	private static int findNextOffset(int[] adj) {
		return adj.length;
	}
}