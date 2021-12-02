package Lab4;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String mode = null;
        int k = 3;
        String distance = "e2";
        boolean unitw = false;
        String train_file = null;
        String test_file = null;
        String kmeans_file = null;
        List<Point> centroids = new ArrayList<>();

        for (int i = 0; i < args.length; ++i) {
            String str = args[i];
            switch (str) {
                case "-mode":
                    mode = args[++i];
                    break;
                case "-k":
                    k = Integer.parseInt(args[++i]);
                    break;
                case "-d":
                    distance = args[++i];
                    break;
                case "-unitw":
                    unitw = true;
                    break;
                case "-train":
                    train_file = args[++i];
                    break;
                case "-test":
                    test_file = args[++i];
                    break;
                case "-data":
                    kmeans_file = args[++i];
                    break;
                default:
                    if (mode != null && mode.equals("kmeans")) {
                        String[] arr = args[i].split(",");
                        List<Double> list = new ArrayList<>();
                        for (String s : arr) {
                            list.add(Double.parseDouble(s));
                        }
                        Point centroid = new Point("", list);
                        centroids.add(centroid);
                        break;
                    } else {
                        System.out.println("Invalid input.");
                        return;
                    }
            }
        }

        if (mode == null) {
            System.out.println("Please input the mode: knn or kmeans.");
            return;
        }

        if (mode.equals("knn") && (train_file == null || test_file == null)) {
            System.out.println("Please input the file path for knn.");
            return;
        }

        if (mode.equals("kmeans") && kmeans_file == null) {
            System.out.println("Please input the file path for kmeans.");
            return;
        }

        if (mode.equals("knn")) {
            List<Point> train = readFile(train_file);
            List<Point> test = readFile(test_file);
            KNN knn = new KNN(train, test, k, distance.equals("e2"), unitw);
        } else if (mode.equals("kmeans")) {
            List<Point> data = readFile(kmeans_file);
            KMeans kMeans = new KMeans(centroids, data, distance.equals("e2"));
        } else {
            System.out.println("Please input the valid mode: knn or kmeans.");
        }
    }

    private static List<Point> readFile(String file) {
        List<Point> points = new ArrayList<>();
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                //System.out.println(strLine);
                String[] arr = strLine.split(",");
                List<Double> list = new ArrayList<>();
                for (int i = 0; i < arr.length - 1; ++i) {
                    list.add(Double.parseDouble(arr[i]));
                }
                Point point = new Point(arr[arr.length - 1], list);
                points.add(point);
            }
            in.close();
        }catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        return points;
    }
}

class KNN {
    List<Point> train, test;
    int k;
    boolean e2;
    boolean unitw;
    Map<String, int[]> labels; // [CQ, C, Q]

    public KNN(List<Point> train, List<Point> test, int k, boolean e2, boolean unitw) {
        this.train = train;
        this.test = test;
        this.k = k;
        this.e2 = e2;
        this.unitw = unitw;
        labels = new HashMap<>();

        print();
    }

    private void print() {
        for (Point point : test) {
            String predictLabel = predict(point);
            System.out.format("want=%s got=%s\n", point.label, predictLabel);

            labels.putIfAbsent(point.label, new int[3]);
            labels.putIfAbsent(predictLabel, new int[3]);
            labels.get(predictLabel)[0] += point.label.equals(predictLabel) ? 1 : 0; // CQ
            labels.get(predictLabel)[2]++; // Q
            labels.get(point.label)[1]++; // C
        }

        for (String label : labels.keySet()) {
            int[] cur = labels.get(label);
            System.out.format("Label=%s Precision=%d/%d Recall=%d/%d\n", label, cur[0], cur[2], cur[0], cur[1]);
        }
    }

    private String predict(Point point) {
        train.sort((a, b) -> Double.compare(getDistance(a, point), getDistance(b, point)));
        Map<String, Double> scores = new HashMap<>();
        for (int i = 0; i < k; ++i) {
            //System.out.println(train.get(i).toString());

            String label = train.get(i).label;

            if (unitw) {
                scores.put(label, scores.getOrDefault(label, 0.0) + 1.0);
            } else {
                double d = getDistance(train.get(i), point);
                scores.put(label, scores.getOrDefault(label, 0.0) + 1.0 / Math.max(d, 0.0001));
            }
        }

        String predictLabel = null;
        double maxScore = 0.0;
        for (String label : scores.keySet()) {
            if (scores.get(label) > maxScore) {
                maxScore = scores.get(label);
                predictLabel = label;
            }
        }
        return predictLabel;
    }

    private double getDistance(Point a, Point b) {
        double dist = 0.0;
        for (int i = 0 ; i < a.list.size(); ++i) {
            if (e2) {
                dist += (a.list.get(i) - b.list.get(i)) * (a.list.get(i) - b.list.get(i));
            } else {
                dist += Math.abs(a.list.get(i) - b.list.get(i));
            }
        }
        return dist;
    }
}

class KMeans {
    List<Point> centroids;
    List<Point> data;
    boolean e2;

    public KMeans(List<Point> centroids, List<Point> data, boolean e2) {
        this.centroids = centroids;
        this.data = data;
        this.e2 = e2;

        if (centroids.get(0).list.size() != data.get(0).list.size()) {
            System.out.println("Invalid centroids dimension.");
            return;
        }

        List<List<Integer>> clusters = cluster();
        print(clusters);
    }

    private List<List<Integer>> cluster() {
        int k = centroids.size();
        List<List<Integer>> prev = new ArrayList<>();
        List<List<Integer>> cur = new ArrayList<>();

        for (int i = 0; i < k; ++i) {
            prev.add(new ArrayList<>());
            cur.add(new ArrayList<>());
        }

        while (true) {
            for (int i = 0; i < data.size(); ++i) {
                double minDist = Double.MAX_VALUE;
                int cIdx = -1;
                Point p = data.get(i);
                for (int j = 0; j < k; ++j) {
                    double d = getDistance(centroids.get(j), p);
                    if (d < minDist) {
                        minDist = d;
                        cIdx = j;
                    }
                }
                cur.get(cIdx).add(i);
            }

            if (compare(prev, cur)) break;

            for (int i = 0; i < k; ++i) {
                if (cur.get(i).size() == 0) continue;
                centroids.set(i, getCentroid(cur.get(i)));
            }

            prev = cur;
            cur = new ArrayList<>();
            for (int i = 0; i < k; ++i) {
                cur.add(new ArrayList<>());
            }
        }
        return cur;
    }

    private Point getCentroid(List<Integer> points) {
        int cnt = points.size();
        int dim = data.get(0).list.size();

        List<Double> list = new ArrayList<>();

        for (int i = 0; i < dim; ++i) {
            double cur = 0.0;
            for (Integer point : points) {
                cur += data.get(point).list.get(i);
            }
            list.add(cur / cnt);
        }

        return new Point("", list);
    }

    private boolean compare(List<List<Integer>> prev, List<List<Integer>> cur) {
        for (int i = 0; i < prev.size(); ++i) {
            if (!prev.get(i).equals(cur.get(i))) return false;
        }
        return true;
    }

    private void print(List<List<Integer>> clusters) {
        int k = centroids.size();
        int dim = centroids.get(0).list.size();

        for (int i = 0; i < k; ++i) {
            System.out.format("C%d = {", i + 1);
            int cnt = clusters.get(i).size();
            for (int j = 0; j < cnt - 1; ++j) {
                System.out.print(data.get(clusters.get(i).get(j)).label + ",");
            }
            System.out.println(data.get(clusters.get(i).get(cnt - 1)).label + "}");
        }

        for (Point centroid : centroids) {
            System.out.print("([");
            for (int j = 0; j < dim - 1; ++j) {
                System.out.print(centroid.list.get(j) + " ");
            }
            System.out.println(centroid.list.get(dim - 1) + "])");
        }
    }

    private double getDistance(Point a, Point b) {
        double dist = 0.0;
        for (int i = 0 ; i < a.list.size(); ++i) {
            if (e2) {
                dist += (a.list.get(i) - b.list.get(i)) * (a.list.get(i) - b.list.get(i));
            } else {
                dist += Math.abs(a.list.get(i) - b.list.get(i));
            }
        }
        return dist;
    }
}

class Point {
    String label;
    List<Double> list;

    public Point(String label, List<Double> list) {
        this.label = label;
        this.list = list;
    }

    @Override
    public String toString() {
        return "Point{" +
                "label='" + label + '\'' +
                ", list=" + list +
                '}';
    }
}
