import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {

        for (String s : args) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(s));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(s + ".mod"));

            //region First Line Parsing

            //Parsing of first line
            String line = bufferedReader.readLine();
            String[] tokens = line.split(" ");

            int verticesNumber = Integer.parseInt(tokens[0]);
            int edgesNumber = Integer.parseInt(tokens[1]);
            int numberOfVerticesWithBatteries = Integer.parseInt(tokens[2]);
            int days = Integer.parseInt(tokens[3]);
            int requestsNumber = Integer.parseInt(tokens[4]);


            Vortex[] vertices = new Vortex[verticesNumber];
            for (int i = 0; i < verticesNumber; i++) {
                vertices[i] = new Vortex(i+1);
            }

            //endregion

            //region Edges Parsing

            Edge[] edges = new Edge[edgesNumber];
            for (int i = 0; i < edgesNumber; i++) {
                line = bufferedReader.readLine();

                tokens = line.split(" ");
                int from = Integer.parseInt(tokens[0]);
                int to = Integer.parseInt(tokens[1]);
                edges[i] = new Edge(i+1, vertices[from-1],
                        vertices[to-1],
                        Integer.parseInt(tokens[2]));

                vertices[from-1].edgesFrom.add(edges[i]);
                vertices[to-1].edgesTo.add(edges[i]);
            }

            //endregion

            //region Battery vertices parsing

            line = bufferedReader.readLine();
            tokens = line.split(" ");

            int[] idOfVerticesWithBatteries = new int[numberOfVerticesWithBatteries];
            for (int i = 0; i < numberOfVerticesWithBatteries; i++) {
                idOfVerticesWithBatteries[i] = Integer.parseInt(tokens[i]); // caching
                vertices[idOfVerticesWithBatteries[i]-1].canHaveBattery = true;
            }

            //endregion

            //region Requests Parsing

            for (int i = 0; i < requestsNumber; i++) {
                line = bufferedReader.readLine();
                tokens = line.split(" ");

                vertices[Integer.parseInt(tokens[1])-1].requests
                        .add(new Request(Integer.parseInt(tokens[0]),
                                Integer.parseInt(tokens[2])));
            }

            //endregion

            //region Header
            bufferedWriter.write("# I searched for ropes on ebay while coding this");
            bufferedWriter.newLine(); // I have never experienced such depression
            //endregion

            //region Battery logic

            for (int batteryVortexID = 0; batteryVortexID < numberOfVerticesWithBatteries; batteryVortexID++) {
                Vortex v = vertices[idOfVerticesWithBatteries[batteryVortexID]-1];

                bufferedWriter.write("var BCap_" + v.id + ", >=0;");
                bufferedWriter.newLine();
            }

            for (int day = 1; day < days; day++) {
                for (int batteryVortexID = 0; batteryVortexID < numberOfVerticesWithBatteries; batteryVortexID++) {
                    Vortex v = vertices[idOfVerticesWithBatteries[batteryVortexID] - 1];

                    bufferedWriter.write("var BLvl_" + v.id + "_day_" + day + ", >=0;");
                    bufferedWriter.newLine();
                    bufferedWriter.write("BLimit_" + v.id + "_day_" + day + ": BLvl_" + v.id + "_day_" + day + "<= BCap_" + v.id + ";");
                    bufferedWriter.newLine();
                }
            }
            //endregion

            //region Edge capacities
            for (int edgeIndex = 0; edgeIndex < edgesNumber; edgeIndex++) {
                for (int day = 1; day <= days; day++) {
                    bufferedWriter.write("var edge_" + edges[edgeIndex].id + "_day_" + day + ", >= -" + edges[edgeIndex].capacity + ", <= "
                            + edges[edgeIndex].capacity + ";");
                    bufferedWriter.newLine();
                }
            }
            //endregion

            //region Vortex Kirchhoff rules
            for (int vortex = 1; vortex < verticesNumber; vortex++) {
                for (int day = 1; day <= days; day++) {
                    StringBuilder sb = new StringBuilder("vortex_" + vertices[vortex].id + "_day_" + day + ": ");
                    if (vertices[vortex].requests.isEmpty()) {
                        sb.append("0 <=");
                    }
                    else {
                        final int xd = day;
                        Optional<Request> temp = vertices[vortex].requests.stream().filter(x -> x.day == xd).findFirst();

                        if (temp.isPresent()){
                            sb.append(temp.get().usage).append(" <=");
                        }
                        else {
                            sb.append("0 <=");
                        }
                    }
                    for (Edge e: vertices[vortex].edgesFrom) {
                        sb.append(" - edge_").append(e.id).append("_day_").append(day);
                    }
                    for (Edge e: vertices[vortex].edgesTo) {
                        sb.append(" + edge_").append(e.id).append("_day_").append(day);
                    }
                    if (vertices[vortex].canHaveBattery) {
                        if (day == 1) sb.append("- BLvl_").append(vertices[vortex].id).append("_day_1");
                        else if (day == days) sb.append("+ BLvl_").append(vertices[vortex].id).append("_day_").append(day-1);
                        else sb.append("- BLvl_").append(vertices[vortex].id).append("_day_").append(day)
                                    .append("+ BLvl_").append(vertices[vortex].id).append("_day_").append(day - 1);
                    }
                    sb.append(" - 0;");
                    bufferedWriter.write(sb.toString());
                    bufferedWriter.newLine();
                }
            }
            //endregion

            //region Minimize clause

            StringBuilder sb = new StringBuilder("minimize obj:");
            for (int i = 0; i < numberOfVerticesWithBatteries; i++) {
                sb.append(" BCap_").append(idOfVerticesWithBatteries[i]).append(" +");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append(";");
            bufferedWriter.write(sb.toString());
            bufferedWriter.newLine();
            bufferedWriter.write("solve;");
            bufferedWriter.newLine();
            //endregion

            //region Output

            bufferedWriter.write("printf \"OUTPUT:\\n\";");
            bufferedWriter.newLine();
            bufferedWriter.write("printf \"%d \",obj;");
            bufferedWriter.newLine();
            for (int day = 1; day <= days; day++) {
                bufferedWriter.write("printf \"\\n\";");
                bufferedWriter.newLine();
                for (int i = 1; i <= edgesNumber; i++) {
                    bufferedWriter.write("printf \"%d \",edge_" + i + "_day_" + day + ";");
                    bufferedWriter.newLine();
                }
            }
            bufferedWriter.write("printf \"\\n\";");
            bufferedWriter.newLine();
            //endregion

            bufferedReader.close();
            bufferedWriter.close();
        }
    }
}

//region Data Containers

class Request {
    int day;
    int usage;

    public Request(int day, int usage) {
        this.day = day;
        this.usage = usage;
    }
}

class Vortex {
    int id;
    ArrayList<Edge> edgesFrom = new ArrayList<>();
    ArrayList<Edge> edgesTo = new ArrayList<>();
    ArrayList<Request> requests = new ArrayList<>();
    boolean canHaveBattery;

    public Vortex(int id) {
        this.id = id;
    }
}

class Edge {
    public Edge(int id, Vortex first, Vortex second, int capacity) {
        this.id = id;
        this.first = first;
        this.second = second;
        this.capacity = capacity;
    }

    int id;
    Vortex first;
    Vortex second;
    int capacity;
}

//endregion