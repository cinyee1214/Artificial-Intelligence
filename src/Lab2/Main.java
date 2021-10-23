package Lab2;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
// tests
//        String test1 = "java Main.java -v -mode dpll /Users/cinyee/IdeaProjects/ArtificialIntelligence/src/Lab2/CNF4.txt";
//        args = test1.split(" ");

//        String test2 = "java Main.java -v -mode cnf /Users/cinyee/IdeaProjects/ArtificialIntelligence/src/Lab2/BNF2.txt";
//        args = test2.split(" ");

//        String test2 = "java Main.java -v -mode solver /Users/cinyee/IdeaProjects/ArtificialIntelligence/src/Lab2/BNF2.txt";
//        args = test2.split(" ");

        boolean verbose = false;
        String mode = null, file = null;

        for (int i = 0; i < args.length; ++i) {
            String str = args[i];
            switch (str) {
                case "-v":
                    verbose = true;
                    break;
                case "-mode":
                    mode = args[++i];
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

        if (mode == null) {
            System.out.println("Please input the mode");
            return;
        }

        switch (mode) {
            case "cnf":
                BNF bnf = new BNF(verbose, file);
                bnf.print();
                break;
            case "dpll":
                DPLL dpll = new DPLL(verbose, file);
                boolean res = dpll.solve();
                if (res) {
                    dpll.printRes();
                } else {
                    System.out.println("This is not solvable!");
                }
                break;
            case "solver":
                BNF solver = new BNF(verbose, file);
                DPLL d = new DPLL(verbose, solver.clauses);
                boolean r = d.solve();
                if (r) {
                    d.printRes();
                } else {
                    System.out.println("This is not solvable!");
                }
                break;
            default:
                System.out.println("Please input the valid mode");
        }
    }
}

class DPLL {
    Map<String, Boolean> res;
    Set<String> symbols;
    List<Map<String, Boolean>> clauses;
    boolean verbose;

    public DPLL(boolean verbose, List<Map<String, Boolean>> clauses) {
        res = new HashMap<>();
        symbols = new HashSet<>();
        for (Map<String, Boolean> map : clauses) {
            for (String atom : map.keySet()) {
                symbols.add(atom);
            }
        }
        this.clauses = clauses;
        this.verbose = verbose;
    }

    public DPLL(boolean verbose, String file) throws IOException {
        res = new HashMap<>();
        symbols = new HashSet<>();
        clauses = new ArrayList<>();
        this.verbose = verbose;

        try{
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                String[] arr = strLine.split(" ");
                Map<String, Boolean> cur = new HashMap<>();
                for (String str : arr) {
                    if (str.startsWith("!")) {
                        cur.put(str.substring(1), false);
                        symbols.add(str.substring(1));
                    } else {
                        cur.put(str, true);
                        symbols.add(str);
                    }
                }
                clauses.add(cur);
            }
            in.close();
        }catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
    }

    public boolean solve() {
        if (verbose) {
            for (Map<String, Boolean> clause : clauses) {
                for (String key : clause.keySet()) {
                    boolean val = clause.get(key);
                    System.out.print(!val ? "!" : "");
                    System.out.print(key + " ");
                }
                System.out.println();
            }
        }

        Map<String, Integer> map = new HashMap<>(); // 1:true, -1:false, 0:both
        for (Map<String, Boolean> clause : clauses) {
            if (clause.size() == 1) return singleAtom(clause);

            for (String key : clause.keySet()) {
                Integer cur = map.get(key);
                boolean val = clause.get(key);
                if (cur == null) {
                    map.put(key, val ? 1 : -1);
                } else if ((cur == 1 && !val) || (cur == -1 && val)) {
                    map.put(key, 0);
                }
            }
        }

        if (map.size() == 0) return true;

        Map.Entry<String, Integer> entry = pureLiteral(map);
        if (entry != null) {
            String atom = entry.getKey();
            boolean val = entry.getValue() == 1;

            if (verbose) System.out.println("easyCase: pure literal " + atom + " = " + val);

            res.put(atom, val);
            List<Map<String, Boolean>> cur = new ArrayList<>();
            for (Map<String, Boolean> clause : clauses) {
                if (!clause.containsKey(atom)) {
                    cur.add(clause);
                }
            }
            if (cur.size() == 0) return true;
            clauses = cur;
            return solve();
        }

        return hardCase(map);
    }

    private boolean singleAtom(Map<String, Boolean> cur) {
        String atom = null;
        boolean val = false;
        for (String key : cur.keySet()) {
            atom = key;
            val = cur.get(atom);
        }
        res.put(atom, val);
        if (verbose) System.out.println("easyCase: unit literal " + atom + " = " + val);

        if (!helper(atom, val)) return false;
        if (clauses.isEmpty()) return true;

        return solve();
    }

    private boolean helper(String atom, boolean val) {
        List<Map<String, Boolean>> tmp = new ArrayList<>();
        for (Map<String, Boolean> clause : clauses) {
            if (clause.size() == 1 && clause.containsKey(atom) && clause.get(atom) != val) {
                res.remove(atom);
                String str = !clause.get(atom) ? "!" : "";
                if (verbose) System.out.println(str + atom + " contradiction");
                return false;
            }
            Boolean curVal = clause.get(atom);
            if (curVal != null) {
                if (curVal == val) continue;
                clause.remove(atom);
            }
            if (!clause.isEmpty()) tmp.add(clause);
        }

        clauses = tmp;
        return true;
    }

    private Map.Entry<String, Integer> pureLiteral(Map<String, Integer> map) {
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>((a, b) -> a.getKey().compareTo(b.getKey()));
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() != 0) pq.offer(entry);
        }
        return pq.isEmpty() ? null : pq.poll();
    }

    private boolean hardCase(Map<String, Integer> map) {
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>((a, b) -> a.getKey().compareTo(b.getKey()));
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            pq.offer(entry);
        }
        Map.Entry<String, Integer> entry = pq.poll();
        String atom = entry.getKey();

        // guest true
        if (verbose) System.out.println("hardCase, guess: " + atom + " = " + true);
        res.put(atom, true);

        List<Map<String, Boolean>> prev = new ArrayList<>();
        for (Map<String, Boolean> clause : clauses) prev.add(new HashMap<>(clause));

        if (!helper(atom, true)) return false;
        if (clauses.isEmpty()) return true;

        if (!solve()) {
            res.put(atom, false);
            if (verbose) System.out.println("fail|hardCase, try: " + atom + " = " + false);

            clauses = prev;
            if (!helper(atom, false)) return false;
            if (clauses.isEmpty()) return true;

            return solve();
        }

        return true;
    }

    public void printRes() {
        for (String symbol : symbols) {
            if (!res.containsKey(symbol)) {
                if (verbose) System.out.println("unbound " + symbol + " = " + false);
                res.put(symbol, false);
            }
        }
        PriorityQueue<Map.Entry<String, Boolean>> pq = new PriorityQueue<>((a, b) -> a.getKey().compareTo(b.getKey()));
        for (Map.Entry<String, Boolean> entry : res.entrySet()) {
            pq.offer(entry);
        }
        while (!pq.isEmpty()) {
            Map.Entry<String, Boolean> entry = pq.poll();
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }
}

class BNF {
    List<Map<String, Boolean>> clauses;
    boolean verbose;
    String[] ops;

    public BNF(boolean verbose, String file) {
        clauses = new ArrayList<>();
        this.verbose = verbose;
        ops = new String[]{"<=>", "=>", "|", "&"};

        List<String> list = new ArrayList<>();

        try{
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                Node node = construct(strLine.replaceAll(" ", ""));
                resolve(node);
            }
            in.close();
        }catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
    }

    private Node construct(String bnf) {
        return parse(bnf, 0);
    }

    private Node parse(String text, int opIdx) {
        if (opIdx == 4) {
            boolean sign = !text.startsWith("!");
            text = sign ? text : text.substring(1);
            return new Node(text, sign);
        }

        String op = ops[opIdx];
        int idx = text.lastIndexOf(op);
        if (idx == -1) return parse(text, opIdx + 1);

        Node left = parse(text.substring(0, idx), 0);
        Node right = parse(text.substring(idx + op.length()), opIdx + 1);
        return new Node(op, true, left, right);
    }

    private void resolve(Node node) {
        if (verbose) System.out.println("BNF:\n" + node.toString());

        Node res = eliminateIff(node);
        if (verbose) System.out.println("Eliminate <=>\n" + res.toString());
        res = eliminateImply(res);
        if (verbose) System.out.println("Eliminate =>\n" + res.toString());
        res = deMorgan(res);
        if (verbose) System.out.println("DeMorgan's Law\n" + res.toString());
        res = distribution(res);
        if (verbose) System.out.println("Distribution\n" + res.toString());

        if (verbose) System.out.println("------------------Converted-----------------");
        List<Map<String, Boolean>> list = convertToCNF(res);
        clauses.addAll(list);
    }

    private List<Map<String, Boolean>> convertToCNF(Node node) {
        List<Map<String, Boolean>> res = new ArrayList<>();
        if (node.isAtom()) {
            Map<String, Boolean> map = new HashMap<>();
            map.put(node.atom, node.sign);
            res.add(map);
            return res;
        }

        List<Map<String, Boolean>> left = convertToCNF(node.left);
        List<Map<String, Boolean>> right = convertToCNF(node.right);

        if (node.op.equals("&")) {
            res.addAll(left);
            res.addAll(right);
            return res;
        }

        // node.op.equals("|")
        if (left.size() == 0 || right.size() == 0) return res;

        Map<String, Boolean> map = new HashMap<>();
        for (Map<String, Boolean> leftMap : left) {
            for (Map.Entry<String, Boolean> entry : leftMap.entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map<String, Boolean> rightMap : right) {
            for (Map.Entry<String, Boolean> entry : rightMap.entrySet()) {
                if (map.containsKey(entry.getKey()) && map.get(entry.getKey()) != entry.getValue()) return res;
                map.put(entry.getKey(), entry.getValue());
            }
        }
        res.add(map);
        return res;
    }

    private Node distribution(Node node) {
//        System.out.println("line 346: " + node.toString());
        if (node.isAtom()) return node;

        Node left = distribution(node.left);
        Node right = distribution(node.right);
//        System.out.println("line 351: " + left.toString());
//        System.out.println("line 352: " + right.toString());
//        System.out.println("line 353: " + node.op);
//        System.out.println("line 354: " + node.sign);

        if (node.op.equals("|") && !left.isAtom && left.op.equals("&")) {
            Node cur = new Node("&", node.sign,
                                    distribution(new Node("|", true, left.left, right)),
                                    distribution(new Node("|", true, left.right, right)));
            return cur;
        }
        if (node.op.equals("|") && !right.isAtom() && right.op.equals("&")) {
            //System.out.println("line 361");
            Node cur = new Node("&", node.sign,
                                    distribution(new Node("|", true, left, right.left)),
                                    distribution(new Node("|", true, left, right.right)));
           // System.out.println("line 364: " + cur.toString());
            return cur;
        }
        return new Node(node.op, node.sign, left, right);
    }

    private Node deMorgan(Node node) {
        //System.out.println("line 346: " + node.toString());
        if (node.isAtom()) return node;

        Node left = deMorgan(node.left);
        Node right = deMorgan(node.right);
//        System.out.println("line 351: " + left.toString());
//        System.out.println("line 352: " + right.toString());
//        System.out.println("line 353: " + node.op);
//        System.out.println("line 354: " + node.sign);

        if (!node.sign && node.op.equals("|")) {
            Node res = new Node("&", true,
                    deMorgan(new Node(left)),
                    deMorgan(new Node(right)));
            //System.out.println("line 360: " + res.toString());
            return res;
        }

        if (!node.sign && node.op.equals("&")) {
            Node res = new Node("|", true,
                    deMorgan(new Node(left)),
                    deMorgan(new Node(right)));
            //System.out.println("line 368: " + res.toString());
            return res;
        }

        return new Node(node.op, node.sign, left, right);
    }

    private Node eliminateImply(Node node) {
        if (node.isAtom()) return node;

        Node left = eliminateImply(node.left);
        Node right = eliminateImply(node.right);

        if (!node.op.equals("=>")) return new Node(node.op, node.sign, left, right);

        Node newLeft = new Node(left);
        return new Node("|", node.sign, newLeft, right);
    }

    private Node eliminateIff(Node node) {
        if (node.isAtom()) return node;

        Node left = eliminateIff(node.left);
        Node right = eliminateIff(node.right);

        if (!node.op.equals("<=>")) return new Node(node.op, node.sign, left, right);

        return new Node("&", node.sign,
                                    new Node("=>", true, left, right),
                                    new Node("=>", true, right, left));
    }

    public void print() {
        for (Map<String, Boolean> clause : clauses) {
            for (String key : clause.keySet()) {
                boolean val = clause.get(key);
                System.out.print(!val ? "!" : "");
                System.out.print(key + " ");
            }
            System.out.println();
        }
    }
}

class Node {
    String op;
    boolean sign;
    Node left;
    Node right;
    String atom;
    boolean isAtom;

    public Node(String atom, boolean sign) {
        this.atom = atom;
        this.sign = sign;
        isAtom = true;
    }

    public Node(String op, boolean sign, Node left, Node right) {
        this.op = op;
        this.sign = sign;
        this.left = left;
        this.right = right;
        isAtom = false;
    }

    public Node(Node node) {
        sign = !node.sign;
        if (node.isAtom()) {
            isAtom = true;
            atom = node.atom;
        } else {
            op = node.op;
            left = node.left;
            right = node.right;
            isAtom = false;
        }
    }

    public boolean isAtom() {
        return isAtom;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        inorder(this, sb);
        return sb.toString();
    }

    private void inorder(Node node, StringBuilder sb) {
        if (node.isAtom()) {
            if (!node.sign) sb.append("!");
            sb.append(node.atom);
            return;
        }

        if (!node.sign) sb.append("!");
        inorder(node.left, sb);
        sb.append(node.op);
        inorder(node.right, sb);
    }
}
