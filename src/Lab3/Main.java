package Lab3;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        double df = 1.0, tol = 0.01;
        boolean min = false;
        int iter = 100;
        String file = null;

        for (int i = 0; i < args.length; ++i) {
            String str = args[i];
            switch (str) {
                case "-df":
                    df = Double.parseDouble(args[++i]);
                    break;
                case "-min":
                    min = true;
                    break;
                case "-tol":
                    tol = Double.parseDouble(args[++i]);
                    break;
                case "-iter":
                    iter = Integer.parseInt(args[++i]);
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

        Map<Node, Double> nodes = new HashMap<>();
        Map<String, Node> nameToNode = new HashMap<>();

        try{
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                if (strLine.startsWith("#")) continue;
                //System.out.println(strLine);
                if (strLine.contains("=")) {
                    String[] arr = strLine.split("=");
                    String name = arr[0].trim();
                    int reward = Integer.parseInt(arr[1]);
                    nameToNode.putIfAbsent(name, new Node(name));
                    Node node = nameToNode.get(name);

                    node.reward = reward;
                    nodes.put(node, (double)reward);
                } else if (strLine.contains("%")) {
                    String[] arr = strLine.split("%");
                    String name = arr[0].trim();
                    //System.out.println("line 65: " + name);
                    nameToNode.putIfAbsent(name, new Node(name));
                    Node node = nameToNode.get(name);

                    String[] probs = arr[1].split(" ");
                    //System.out.println("line 70: " + probs.length);
                    double sum = 1.0;
                    int cnt = 0;
                    for (String prob : probs) {
                        if (prob.equals("")) continue;
                        double p = Double.parseDouble(prob);
                        // System.out.println("line 75: " + p);
                        sum -= p;
                        //System.out.println(sum);
                        cnt++;
                        node.probs.add(p);
                    }
                    if (cnt == 1) {
                        node.decision = true;
                        node.decisionProb = node.probs.get(0);
                    } else {
                        if (sum > 1e-10) {
                            System.out.println("Invalid input, probabilities should sum to 1:");
                            System.out.println(node.name);
                            return;
                        }
                    }
                    nodes.putIfAbsent(node, 0.0);
                } else if (strLine.contains(":")) {
                    String[] arr = strLine.split(":");
                    String name = arr[0].trim();
                    nameToNode.putIfAbsent(name, new Node(name));
                    Node node = nameToNode.get(name);

                    String arrString = arr[1];
                    int j = 0;
                    while (j < arrString.length() && arrString.charAt(j) != '[') j++;

                    String[] outs = arrString.substring(j + 1, arrString.length() - 1).split(", ");
                    for (String out : outs) {
                        nameToNode.putIfAbsent(out, new Node(out));
                        Node outNode = nameToNode.get(out);
                        nodes.putIfAbsent(outNode, 0.0);
                        node.outs.add(outNode);
                    }
                    nodes.putIfAbsent(node, 0.0);
                }
            }
            in.close();
        }catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        boolean hasOptimalPolicy = false;
        for (Map.Entry<Node, Double> entry : nodes.entrySet()) {
            Node node = entry.getKey();
            double val = entry.getValue();
            if (node.outs.size() == 0) {
                if (node.probs.size() != 0) {
                    System.out.println("Invalid input, a probability entry for a terminate node is invalid.");
                    return;
                }
                node.terminate = true;
            }
            if (!node.decision && !node.terminate) {
                if (node.outs.size() > 1 && node.probs.size() == 0) {
                    node.decision = true;
                    node.decisionProb = 1.0;
                } else if (node.outs.size() == 1 && node.probs.size() == 0) {
                    node.probs.add(1.0);
                } else if (node.outs.size() != node.probs.size()) {
                    System.out.println(node.name);
                    System.out.println(node.outs.size());
                    System.out.println(node.probs.size());
                    System.out.println("Invalid input, a probability entry cannot match the out edges.");
                    return;
                }
            }
            if (node.decision) hasOptimalPolicy = true;
//            System.out.println(node.name);
//            System.out.println(node.decision);
//            System.out.println(node.outs.size());
            //System.out.println(node.toString());
        }

        MDP mdp = new MDP(nodes, nameToNode, min, df, tol, iter, hasOptimalPolicy);
        mdp.solver();
    }

}

class MDP {
    Map<Node, Double> nodes;
    Map<String, Node> nameToNode;
    boolean min;
    double discountFactor;
    double tolerance;
    int iteration;
    boolean hasOptimalPolicy; // a problem with only chance nodes has no policy

    public MDP(Map<Node, Double> nodes, Map<String, Node> nameToNode, boolean min, double discountFactor, double tolerance, int iteration, boolean hasOptimalPolicy) {
        this.nodes = nodes;
        this.nameToNode = nameToNode;
        this.min = min;
        this.discountFactor = discountFactor;
        this.tolerance = tolerance;
        this.iteration = iteration;
        this.hasOptimalPolicy = hasOptimalPolicy;
    }

    @Override
    public String toString() {
        return "MDP{" +
                "min=" + min +
                ", discountFactor=" + discountFactor +
                ", tolerance=" + tolerance +
                ", iteration=" + iteration +
                ", hasOptimalPolicy=" + hasOptimalPolicy +
                '}';
    }

    public void solver() {
        //System.out.println(toString());

        Map<Node, Double> cur = new HashMap<>(nodes);

        for (int n = 1; n < iteration; ++n) {
            Map<Node, Double> nxt = new HashMap<>();
            for (Map.Entry<Node, Double> entry : nodes.entrySet()) {
                Node node = entry.getKey();
                double prob = entry.getValue();
                if (node.terminate) {
                    nxt.put(node, prob);
                    continue;
                }
                if (node.decision) {
                    int dmin = -1, dmax = -1;
                    double pmin = Double.MAX_VALUE, pmax = Double.MIN_VALUE;
                    for (int i = 0; i < node.outs.size(); ++i) {
                        Node prev = node.outs.get(i);
                        double p = cur.get(prev);
                        if (p < pmin) {
                            pmin = p;
                            dmin = i;
                        }
                        if (p > pmax) {
                            pmax = p;
                            dmax = i;
                        }
                    }
                    int di = min ? dmin : dmax;
                    for (int i = 0; i < node.outs.size(); ++i) {
                        Node prev = node.outs.get(i);
                        if (i == di) {
                            prob += discountFactor * node.decisionProb * cur.get(prev);
                        } else {
                            prob += discountFactor * (1 - node.decisionProb) / (node.outs.size() - 1) * cur.get(prev);
                        }
                    }
                    nxt.put(node, prob);
                    continue;
                }
                // chance nodes
                for (int i = 0; i < node.outs.size(); ++i) {
                    Node prev = node.outs.get(i);
                    prob += discountFactor * node.probs.get(i) * cur.get(prev);
                }
                nxt.put(node, prob);
            }
            boolean stop = true;
            for (Map.Entry<Node, Double> entry : nodes.entrySet()) {
                Node node = entry.getKey();
                if (Math.abs(cur.get(node) - nxt.get(node)) > tolerance) {
                    stop = false;
                    break;
                }
            }
            cur = nxt;
            if (stop) {
                //System.out.println(n);
                break;
            }
        }

        if (hasOptimalPolicy) printPolicy(cur);
        printValues(cur);
    }

    private void printValues(Map<Node, Double> cur) {
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> a.name.compareTo(b.name));
        for (Node node : cur.keySet()) {
            pq.offer(node);
        }
        while (!pq.isEmpty()) {
            Node node = pq.poll();
            System.out.format("%s=%.3f ", node.name, cur.get(node));
        }
        System.out.println();
    }

    private void printPolicy(Map<Node, Double> cur) {
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> a.name.compareTo(b.name));
        for (Node node : cur.keySet()) {
            pq.offer(node);
        }
        while (!pq.isEmpty()) {
            Node node = pq.poll();
            if (!node.decision) continue;
            double pmin = Double.MAX_VALUE, pmax = Double.MIN_VALUE;
            Node nmin = null, nmax = null;
            //System.out.println(node.name);
            for (int i = 0; i < node.outs.size(); ++i) {
                Node next = node.outs.get(i);
                double p = cur.get(next);
                //System.out.println(next.name + ":" + p);
                if (p < pmin) {
                    pmin = p;
                    nmin = next;
                }
                if (p > pmax) {
                    pmax = p;
                    nmax = next;
                }
            }
            if (min) {
                System.out.println(node.name + " -> " + nmin.name);
                //System.out.println(cur.get(nmin));
            } else {
                System.out.println(node.name + " -> " + nmax.name);
                //System.out.println(cur.get(nmax));
            }
        }
        System.out.println();
    }
}

class Node {
    String name;
    List<Node> outs;
    int reward;
    List<Double> probs;
    boolean terminate;
    boolean decision;
    double decisionProb;

    public Node(String name) {
        this.name = name;
        outs = new ArrayList<>();
        reward = 0;
        probs = new ArrayList<>();
        terminate = false;
        decision = false;
        decisionProb = 1.0;
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                ", outs=" + outs.size() +
                ", reward=" + reward +
                ", probs=" + probs +
                ", terminate=" + terminate +
                ", decision=" + decision +
                ", decisionProb=" + decisionProb +
                '}';
    }
}
