package org.rest.Respector.archive;

import java.util.*;
import java.util.stream.Collectors;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;

public class MyAppOld extends SceneTransformer {
    private List<Pair<SootMethod, List<Integer>>> endpointsWithParam;
    private Map<Body, BriefUnitPrinter> printerSet;
    private JimpleBasedInterproceduralCFG icfg;
    private Set<Unit> visitedUnits; // To track visited units and avoid infinite loops

    public MyAppOld(List<Pair<SootMethod, List<Integer>>> listEndpointsWithParam) {
        this.endpointsWithParam = listEndpointsWithParam;
        this.printerSet = new HashMap<>();
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        icfg = new JimpleBasedInterproceduralCFG();
        visitedUnits = new HashSet<>();

        endpointsWithParam.forEach(endpoint -> {
            SootMethod method = endpoint.getO1();
            List<Integer> parameterIndices = endpoint.getO2();
            List<List<Pair<Stmt, String>>> traces = traverseMethod(method, parameterIndices, new ArrayList<>());

            traces.forEach(this::printPath);
        });
    }

    private List<List<Pair<Stmt, String>>> traverseMethod(SootMethod method, List<Integer> parameterIndices, List<SootMethod> callStack) {
        JimpleBody body = (JimpleBody) method.retrieveActiveBody();
        UnitGraph graph = new ExceptionalUnitGraph(body);

        Set<Local> flowSet = parameterIndices.stream()
            .map(index -> body.getParameterLocals().get(index))
            .collect(Collectors.toSet());

        List<Unit> heads = graph.getHeads();
        List<List<Pair<Stmt, String>>> paths = new ArrayList<>();
        for (Unit head : heads) {
            paths.addAll(traverseStatement((Stmt) head, new ArrayList<>(), flowSet, new ArrayList<>(callStack)));
        }
        return paths;
    }

    private List<List<Pair<Stmt, String>>> traverseStatement(Stmt stmt, List<Pair<Stmt, String>> path, Set<Local> flowSet, List<SootMethod> callStack) {
        if (!visitedUnits.add(stmt)) {
            return new ArrayList<>();
        }

        List<List<Pair<Stmt, String>>> traces = new ArrayList<>();
        List<Pair<Stmt, String>> newPath = new ArrayList<>(path);

        do {
            newPath.add(new Pair<>(stmt, interpretStatement(stmt, flowSet)));
            List<Unit> successors = icfg.getSuccsOf(stmt);

            if (stmt.containsInvokeExpr()) {
                handleInvokeExpression(stmt, flowSet, callStack, newPath, traces);
                break;
            } else if (successors.isEmpty()) {
                traces.add(newPath);
                break;
            } else if (successors.size() == 1) {
                stmt = (Stmt) successors.get(0);
            } else {
                successors.forEach(succ -> traces.addAll(traverseStatement((Stmt) succ, newPath, flowSet, callStack)));
                break;
            }
        } while (!stmt.containsInvokeExpr());

        visitedUnits.remove(stmt);
        return traces;
    }

    private void handleInvokeExpression(Stmt stmt, Set<Local> flowSet, List<SootMethod> callStack, List<Pair<Stmt, String>> path, List<List<Pair<Stmt, String>>> traces) {
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        Collection<SootMethod> callees = icfg.getCalleesOfCallAt(stmt);

        for (SootMethod callee : callees) {
            if (!callee.isPhantom()) {
                List<Integer> subParamIndices = determineParameterIndices(invokeExpr, flowSet);
                List<List<Pair<Stmt, String>>> subTraces = traverseMethod(callee, subParamIndices, callStack);
                subTraces.forEach(subTrace -> {
                    List<Pair<Stmt, String>> combinedPath = new ArrayList<>(path);
                    combinedPath.addAll(subTrace);
                    traces.add(combinedPath);
                });
            }
        }
