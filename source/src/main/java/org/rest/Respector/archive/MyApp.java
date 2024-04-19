package org.rest.Respector.archive;

import java.util.*;
import java.util.stream.Collectors;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;

public class MyApp extends SceneTransformer {
    private List<Pair<SootMethod, List<Integer>>> methodParameters;
    private Map<Body, BriefUnitPrinter> bodyPrinters;
    private JimpleBasedInterproceduralCFG interproceduralCFG;
    private Set<Unit> visitedUnits;

    public MyApp(List<Pair<SootMethod, List<Integer>>> methodParameters) {
        this.methodParameters = methodParameters;
        this.bodyPrinters = new HashMap<>();
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        setupAnalysis();
        methodParameters.forEach(this::analyzeMethodEndpoint);
    }

    private void setupAnalysis() {
        interproceduralCFG = new JimpleBasedInterproceduralCFG();
        visitedUnits = new HashSet<>();
    }

    private void analyzeMethodEndpoint(Pair<SootMethod, List<Integer>> methodParamPair) {
        SootMethod method = methodParamPair.getO1();
        List<Integer> parameterIndices = methodParamPair.getO2();
        if (method.isConcrete()) {
            List<List<Pair<Stmt, String>>> methodTraces = analyzeMethod(method, parameterIndices);
            methodTraces.forEach(this::printTrace);
        }
    }

    private List<List<Pair<Stmt, String>>> analyzeMethod(SootMethod method, List<Integer> parameterIndices) {
        Body methodBody = method.retrieveActiveBody();
        UnitGraph graph = new ExceptionalUnitGraph(methodBody);
        Set<Local> parameterLocals = parameterIndices.stream()
            .map(methodBody::getParameterLocal)
            .collect(Collectors.toSet());

        return graph.getHeads().stream()
            .flatMap(head -> analyzeStatement((Stmt) head, new ArrayList<>(), parameterLocals, new ArrayList<>()).stream())
            .collect(Collectors.toList());
    }

    private List<List<Pair<Stmt, String>>> analyzeStatement(Stmt statement, List<Pair<Stmt, String>> path, Set<Local> parameters, List<SootMethod> callStack) {
        if (!visitedUnits.add(statement)) {
            return Collections.emptyList();
        }

        List<List<Pair<Stmt, String>>> traces = new ArrayList<>();
        Pair<Stmt, String> step = new Pair<>(statement, interpretStatement(statement, parameters));
        List<Pair<Stmt, String>> newPath = new ArrayList<>(path);
        newPath.add(step);

        List<Unit> successors = interproceduralCFG.getSuccsOf(statement);
        handleSuccessors(successors, statement, newPath, parameters, callStack, traces);
        visitedUnits.remove(statement);
        
        return traces;
    }

    private void handleSuccessors(List<Unit> successors, Stmt statement, List<Pair<Stmt, String>> newPath, Set<Local> parameters, List<SootMethod> callStack, List<List<Pair<Stmt, String>>> traces) {
        if (statement.containsInvokeExpr()) {
            handleInvocation(statement, newPath, parameters, callStack, traces);
        } else if (successors.isEmpty()) {
            traces.add(newPath);
        } else {
            successors.forEach(successor -> traces.addAll(analyzeStatement((Stmt) successor, newPath, parameters, callStack)));
        }
    }

    private void handleInvocation(Stmt statement, List<Pair<Stmt, String>> path, Set<Local> parameters, List<SootMethod> callStack, List<List<Pair<Stmt, String>>> traces) {
        InvokeExpr invokeExpr = statement.getInvokeExpr();
        interproceduralCFG.getCalleesOfCallAt(statement).stream()
            .filter(callee -> !callee.isPhantom() && !callStack.contains(callee))
            .forEach(callee -> {
                List<Integer> relevantIndices = determineRelevantIndices(invokeExpr, parameters);
                List<SootMethod> updatedCallStack = new ArrayList<>(callStack);
                updatedCallStack.add(callee);
                List<List<Pair<Stmt, String>>> subTraces = analyzeMethod(callee, relevantIndices);
                subTraces.forEach(subTrace -> {
                    List<Pair<Stmt, String>> combinedPath = new ArrayList<>(path);
                    combinedPath.addAll(subTrace);
                    traces.add(combinedPath);
                });
            });
    }

    private List<Integer> determineRelevantIndices(InvokeExpr expr, Set<Local> parameters) {
        return IntStream.range(0, expr.getArgCount())
            .filter(idx -> parameters.contains(expr.getArg(idx)))
            .boxed()
            .collect(Collectors.toList());
    }

    private String interpretStatement(Stmt stmt, Set<Local> parameters) {
        // Interpret and return a descriptive string based on the statement and parameters
        return "Standard operation";
    }

    private void printTrace(List<Pair<Stmt, String>> trace) {
        // Print or log the trace information for debugging or analysis purposes
    }
}
