package Lab1;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        boolean verbose = false;
        String startLable = null, endLabel = null, algo = null, file = null;
        int depth = -1;

        for (int i = 0; i < args.length; ++i) {
            String str = args[i];
            switch (str) {
                case "-v":
                    verbose = true;
                    break;
                case "-start":
                    startLable = args[++i];
                    break;
                case "-goal":
                    endLabel = args[++i];
                    break;
                case "-depth":
                    depth = Integer.parseInt(args[++i]);
                    break;
                case "-alg":
                    algo = args[++i];
                    break;
                default:
                    file = str;
                    break;
            }
        }

        if (file == null) {
            System.out.println("Please input the file path");
            return;
        }

        Set<Node> nodes = new HashSet<>();
        Map<String, Node> map = new HashMap<>();
        Set<List<Node>> edges = new HashSet<>();
        Node start = null, end = null;

        try{
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                if (strLine.startsWith("#")) continue;
                String[] arr = strLine.split(" ");
                if (arr.length == 3) {
                    // node
                    Node cur = new Node(arr[0], Integer.parseInt(arr[1]), Integer.parseInt(arr[2]));
                    if (arr[0].equals(startLable)) {
                        start = cur;
                    }
                    if (arr[0].equals(endLabel)) {
                        end = cur;
                    }
                    nodes.add(cur);
                    map.put(arr[0], cur);
                } else if (arr.length == 2) {
                    // edge
                    Node a = map.get(arr[0]), b = map.get(arr[1]);
                    if (a == null || b == null) {
                        System.out.println("Error: the edge references a vertex not in the file.");
                        return;
                    }
                    edges.add(Arrays.asList(a, b));
                }
                //System.out.println (strLine);
            }
            in.close();
        }catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        if (start == null) {
            System.out.println("Error: -start references a vertex not in the graph-file.");
            return;
        }
        if (end == null) {
            System.out.println("Error: -goal references a vertex not in the graph-file.");
            return;
        }

        FindPath test = new FindPath(nodes, edges, start, end, verbose);

        switch (algo) {
            case "BFS":
                test.BreadthFirstSearch();
                break;
            case "ID":
                if (depth < 0) {
                    System.out.println("Error: invalid depth for ID.");
                    return;
                }
                test.IterativeDeepening(depth);
                break;
            case "ASTAR":
                test.AStar();
                break;
            default:
                System.out.println("Error: invalid algorithm.");
        }
    }
}

class FindPath {
    Map<Node, List<Node>> graph;
    Node start, end;
    boolean verbose;
    public FindPath(Set<Node> nodes, Set<List<Node>> edges, Node start, Node end, boolean verbose) {
        graph = new HashMap<>();
        this.start = start;
        this.end = end;
        this.verbose = verbose;
        for (Node node : nodes) {
            graph.put(node, new ArrayList<>());
        }
        for (List<Node> edge : edges) {
            graph.get(edge.get(0)).add(edge.get(1));
            graph.get(edge.get(1)).add(edge.get(0));
        }
        for (Node node : nodes) {
            graph.get(node).sort(Comparator.comparing(a -> a.label));
        }
    }
    public void BreadthFirstSearch() {
        Queue<Node> queue = new ArrayDeque<>();
        Set<Node> visited = new HashSet<>();
        Map<Node, Node> prev = new HashMap<>();

        queue.offer(start);
        visited.add(start);
        if (verbose) System.out.println("Expanding: " + start.label);
        boolean found = false;

        while (!queue.isEmpty() && !found) {
            Node cur = queue.poll();
            for (Node nei : graph.get(cur)) {
                if (visited.add(nei)) {
                    prev.put(nei, cur);
                    if (nei.equals(end)) {
                        found = true;
                        break;
                    }
                    queue.offer(nei);
                    if (verbose) System.out.println("Expanding: " + nei.label);
                }
            }
        }

        if (found) {
            printPath(prev);
            return;
        }
        System.out.println("Cannot find a path.");
    }

    private void printPath(Map<Node, Node> prev) {
        Deque<Node> stack = new ArrayDeque<>();
        StringBuilder sb = new StringBuilder();
        stack.push(end);
        while (!stack.element().equals(start)) {
            stack.push(prev.get(stack.element()));
        }
        while (!stack.isEmpty() && !stack.element().equals(end)) {
            sb.append(stack.pop().label).append(" -> ");
        }
        sb.append(end.label);
        System.out.println("Solution: " + sb.toString());
    }

    public void IterativeDeepening(int depth) {
        Map<Node, Node> prev = new HashMap<>();
        boolean found = false;
        while (!found) {
            prev = new HashMap<>();
            found = dfs(start, 0, depth++, prev, new HashSet<>());
        }
        printPath(prev);
    }

    private boolean dfs(Node node, int depth, int maxDepth, Map<Node, Node> prev, Set<Node> visited) {
        visited.add(node);

        if (node == end) return true;
        if (depth == maxDepth) {
            if (verbose) System.out.println("hit depth=" + maxDepth + ": " + node.label);
            return false;
        }
        if (verbose) System.out.println("Expand: " + node.label);
        for (Node nei : graph.get(node)) {
            if (!visited.contains(nei)) {
                prev.put(nei, node);
                if (dfs(nei, depth + 1, maxDepth, prev, visited)) return true;
            }
        }
        return false;
    }

    public void AStar() {
        PriorityQueue<Path> pq = new PriorityQueue<>((a, b) -> {
            return a.g + a.h < b.g + b.h ? -1 : 1;
        });
        for (Node nei : graph.get(start)) {
            Path path = new Path(start, nei, end);
            List<Node> list = path.list;
            if (verbose) System.out.format("%s -> %s ; g=%.2f h=%.2f = %.2f\n", list.get(list.size() - 2).label,
                    list.get(list.size() - 1).label, path.g, path.h, path.g + path.h);
            pq.offer(path);
        }

        while (!pq.isEmpty()) {
            Path path = pq.poll();
            List<Node> list = path.list;

            if (list.get(list.size() - 1).equals(end)) {
                System.out.print("Solution: ");
                printAStarPath(list);
                return;
            }
            if (verbose) {
                System.out.print("adding ");
                printAStarPath(list);
            }
            for (Node nei : graph.get(list.get(list.size() - 1))) {
                Path next = new Path(path, nei, end);
                if (verbose) System.out.format("%s -> %s ; g=%.2f h=%.2f = %.2f\n", list.get(list.size() - 1).label,
                        nei.label, next.g, next.h, next.g + next.h);
                if (!path.canAppend(nei)) continue;
                pq.offer(next);
            }
        }
        System.out.println("Cannot find a path.");
    }

    private void printAStarPath(List<Node> list) {
        for (int i = 0; i + 1 < list.size(); ++i) {
            System.out.print(list.get(i).label + " -> ");
        }
        System.out.println(list.get(list.size() - 1).label);
    }
}

class Path {
    List<Node> list;
    Set<Node> set;
    double g, h;
    public Path(Path path, Node node, Node end) {
        list = new ArrayList<>(path.list);
        set = new HashSet<>(path.list);
        list.add(node);
        set.add(node);
        g = path.g + node.getDistance(list.get(list.size() - 2));
        h = node.getDistance(end);
    }
    public Path(Node a, Node b, Node end) {
        list = new ArrayList<>();
        set = new HashSet<>();
        list.add(a);
        list.add(b);
        set.add(a);
        set.add(b);
        g = a.getDistance(b);
        h = b.getDistance(end);
    }

    public boolean canAppend(Node node) {
        return !set.contains(node);
    }
}

class Node {
    String label;
    int x, y;
    public Node(String label, int x, int y) {
        this.label = label;
        this.x = x;
        this.y = y;
    }

    public double getDistance(Node a) {
        return Math.sqrt((a.x - x) * (a.x - x) + (a.y - y) * (a.y - y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return label.equals(node.label) && x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return label.hashCode() * 31 * 31 + x * 31 + y;
    }
}
