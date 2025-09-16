package datasets;

import smile.neighbor.KDTree;
import smile.neighbor.Neighbor;
import smile.math.distance.EuclideanDistance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import util.BitSet;
import java.util.List;
import java.util.Queue;

public class KNNGraph {

    public List<List<Integer>> graph = new ArrayList<>(); //Directed graph
    public List<List<Integer>> graphUndirected = new ArrayList<>(); //Undirected graph
    public List<List<Double>> distances = new ArrayList<>(); //Directed distances

    public KNNGraph(double[][] data, int k) {
        KDTree<double[]> kdTree = new KDTree<>(data, data);
        for (int i = 0; i < data.length; i++) {
            graph.add(new ArrayList<>());
            graphUndirected.add(new ArrayList<>());
            distances.add(new ArrayList<>());
        }
        for (int i = 0; i < data.length; i++) {
            Neighbor[] neighbors = kdTree.search(data[i], k);
            for (Neighbor neighbor : neighbors) {
                if (i != neighbor.index()) {
                    graph.get(i).add(neighbor.index());
                    graphUndirected.get(i).add(neighbor.index());
                    graphUndirected.get(neighbor.index()).add(i);
                    distances.get(i).add(neighbor.distance());
                }
            }
        }
    }

    //Returns a list of distances for edges with one end on each side of the cut.
    public List<Double> getDistancesBetween(BitSet cut) {
        List<Double> distancesBetween = new ArrayList<>();
        for (int i = 0; i < graph.size(); i++) {
            for (int j = 0; j < graph.get(i).size(); j++) {
                if (cut.get(i) != cut.get(graph.get(i).get(j))) {
                    distancesBetween.add(distances.get(i).get(j));
                }
            }
        }
        return distancesBetween;
    }

    public List<List<Integer>> getConnectedComponents() {
        boolean[] visited = new boolean[graph.size()];
        List<List<Integer>> connectedComponents = new ArrayList<>();
        for (int i = 0; i < graph.size(); i++) {
            if (visited[i]) {
                continue;
            }
            connectedComponents.add(new ArrayList<>());
            Queue<Integer> queue = new ArrayDeque<>();
            queue.add(i);
            visited[i] = true;
            connectedComponents.getLast().add(i);
            while (!queue.isEmpty()) {
                int node = queue.poll();
                List<Integer> neighbors = graphUndirected.get(node);
                for (int n : neighbors) {
                    if (!visited[n]) {
                        queue.add(n);
                        visited[n] = true;
                        connectedComponents.getLast().add(n);
                    }
                }
            }
        }
        return connectedComponents;
    }

    public void print() {
        for (int i = 0; i < graph.size(); i++) {
            System.out.println((i + " -> " + graph.get(i)).replace("[", "").replace("]", ""));
        }
    }

}
