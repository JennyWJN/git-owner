package yoshikihigo.tinypdg.scorpio;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import yoshikihigo.tinypdg.ast.TinyPDGASTVisitor;
import yoshikihigo.tinypdg.cfg.node.CFGNodeFactory;
import yoshikihigo.tinypdg.pdg.PDG;
import yoshikihigo.tinypdg.pdg.edge.PDGEdge;
import yoshikihigo.tinypdg.pdg.node.PDGNode;
import yoshikihigo.tinypdg.pdg.node.PDGNodeFactory;
import yoshikihigo.tinypdg.pe.MethodInfo;
import yoshikihigo.tinypdg.scorpio.data.ClonePairInfo;
import yoshikihigo.tinypdg.scorpio.data.PDGPairInfo;
import yoshikihigo.tinypdg.scorpio.io.BellonWriter;
import yoshikihigo.tinypdg.scorpio.io.Writer;

public class GitOwner {
    public final static String projectToDetect = "main";
    //public final static String cloneLink = "https://github.com/nus-cs4218/cs4218-project-ay1920-s2-2020-team16.git";
    public final static String pdgrecord = "./main/pdgrecord.txt";

    public static void main(String[] args) {

        try {

            final Options options = new Options();

            {
                final Option o = new Option("o", "output", true, "output file");
                o.setArgName("file");
                o.setArgs(1);
                o.setRequired(true);
                options.addOption(o);
            }

            {
                final Option s = new Option("s", "size", true, "size");
                s.setArgName("size");
                s.setArgs(1);
                s.setRequired(true);
                options.addOption(s);
            }

            {
                final Option t = new Option("t", "thread", true,
                        "number of threads");
                t.setArgName("thread");
                t.setArgs(1);
                t.setRequired(false);
                options.addOption(t);
            }

            {
                final Option C = new Option("C", "control", true,
                        "use of control dependency");
                C.setArgName("on or off");
                C.setArgs(1);
                C.setRequired(false);
                options.addOption(C);
            }

            {
                final Option D = new Option("D", "data", true,
                        "use of data dependency");
                D.setArgName("on or off");
                D.setArgs(1);
                D.setRequired(false);
                options.addOption(D);
            }

            {
                final Option E = new Option("E", "execution", true,
                        "use of execution dependency");
                E.setArgName("on or off");
                E.setArgs(1);
                E.setRequired(false);
                options.addOption(E);
            }

            {
                final Option M = new Option("M", "merging", true,
                        "merging consecutive similar nodes");
                M.setArgName("on or off");
                M.setArgs(1);
                M.setRequired(false);
                options.addOption(M);
            }

            final CommandLineParser parser = new PosixParser();
            final CommandLine cmd = parser.parse(options, args);

            final String output = cmd.getOptionValue("o");
            final int SIZE_THRESHOLD = Integer
                    .parseInt(cmd.getOptionValue("s"));
            final int NUMBER_OF_THREADS = cmd.hasOption("t") ? Integer
                    .parseInt(cmd.getOptionValue("t")) : 1;

            boolean useOfControl = !cmd.hasOption("C");
            if (!useOfControl) {
                if (cmd.getOptionValue("C").equals("on")) {
                    useOfControl = true;
                } else if (cmd.getOptionValue("C").equals("off")) {
                    useOfControl = false;
                } else {
                    System.err
                            .println("option of \"-C\" must be \"on\" or \"off\".");
                }
            }

            boolean useOfData = !cmd.hasOption("D");
            if (!useOfData) {
                if (cmd.getOptionValue("D").equals("on")) {
                    useOfData = true;
                } else if (cmd.getOptionValue("D").equals("off")) {
                    useOfData = false;
                } else {
                    System.err
                            .println("option of \"-D\" must be \"on\" or \"off\".");
                }
            }

            boolean useOfExecution = !cmd.hasOption("E");
            if (!useOfExecution) {
                if (cmd.getOptionValue("E").equals("on")) {
                    useOfExecution = true;
                } else if (cmd.getOptionValue("E").equals("off")) {
                    useOfExecution = false;
                } else {
                    System.err
                            .println("option of \"-E\" must be \"on\" or \"off\".");
                }
            }

            boolean useOfMerging = !cmd.hasOption("M");
            if (!useOfMerging) {
                if (cmd.getOptionValue("M").equals("on")) {
                    useOfMerging = true;
                } else if (cmd.getOptionValue("M").equals("off")) {
                    useOfMerging = false;
                } else {
                    System.err
                            .println("option of \"-M\" must be \"on\" or \"off\".");
                }
            }

            if (!useOfExecution && useOfMerging) {
                useOfMerging = false;
            }

            //Process p1 = Runtime.getRuntime().exec("git clone " + cloneLink,null,null);
            //p1.waitFor();
            Stack<CommitRecord> commitStack = new Stack<>();
            String filePath = "./" + projectToDetect + "/commit.txt";
            getCommitRecord(commitStack,filePath);

            final long time1 = System.nanoTime();
            System.out.println("start git author ... ");
            PDG[] changedPdgArray;
            PDG[] targetPdgArray;
            PDG[] combinePdgArray;

            final List<File> targetfiles = new ArrayList<>();
            final List<File> changedfiles = new ArrayList<>();

            if(!commitStack.empty()){
                CommitRecord record = commitStack.peek();
                while(record.filePath == null){
                    commitStack.pop();
                    if(!commitStack.empty()){
                        record = commitStack.peek();
                    }
                    else{
                        System.out.println("Less than one valid commit!");
                        return;
                    }
                }

                String[] cmd2 = {"sh","-c","\"cd " + projectToDetect + " && git checkout " + record.commitID + "\""};
                Process p4 = Runtime.getRuntime().exec(cmd2);
                p4.waitFor();
                for(String file : record.filePath){
                    targetfiles.add(new File("./" + projectToDetect + "/" + file));
                }
                final SortedSet<PDG> pdgs = Collections
                        .synchronizedSortedSet(new TreeSet<PDG>());
                generatePDGs(record.author, targetfiles, pdgs, useOfControl, useOfData, useOfExecution,
                        useOfMerging, SIZE_THRESHOLD, NUMBER_OF_THREADS);
                targetPdgArray = pdgs.toArray(new PDG[0]);
                commitStack.pop();
                writeRecord(pdgrecord,targetPdgArray,record.commitID);
            }
            else{
                System.out.println("Less than one commit!");
                return;
            }

            int compareRound = 0;
            while(!commitStack.empty()){
                System.out.println("Round "+compareRound);
                SortedMap<PDG, SortedMap<PDGNode<?>, Integer>> mappingPDGToPDGNodes = Collections
                        .synchronizedSortedMap(new TreeMap<PDG, SortedMap<PDGNode<?>, Integer>>());
                SortedMap<PDG, SortedMap<PDGEdge, Integer>> mappingPDGToPDGEdges = Collections
                        .synchronizedSortedMap(new TreeMap<PDG, SortedMap<PDGEdge, Integer>>());

                CommitRecord record = commitStack.peek();
                if(record.filePath == null){
                    commitStack.pop();
                    continue;
                }
                else {
                    String[] cmd1 = {"sh","-c","\"cd " + projectToDetect + " && git checkout " + record.commitID + "\""};
                    Process p3 = Runtime.getRuntime().exec(cmd1);
                    p3.waitFor();
                    final SortedSet<PDG> pdgs0 = Collections
                            .synchronizedSortedSet(new TreeSet<PDG>());
                    changedfiles.clear();
                    for(String file : record.filePath){
                        File temp = new File("./" + projectToDetect + "/" + file);
                        if(temp.exists()){
                            String actual = temp.getCanonicalFile().getName();
                            String recordname = file.substring(file.lastIndexOf('/') == -1 ? 0 : file.lastIndexOf('/') + 1);
                            if(actual.equals(recordname)){
                                changedfiles.add(temp);
                            }
                        }

                    }

                    generatePDGs(record.author, changedfiles, pdgs0, useOfControl, useOfData, useOfExecution,
                            useOfMerging, SIZE_THRESHOLD, NUMBER_OF_THREADS);
                    changedPdgArray = pdgs0.toArray(new PDG[0]);
                }

                combinePdgArray = new PDG[targetPdgArray.length + changedPdgArray.length];
                System.arraycopy(targetPdgArray,0,combinePdgArray,0,targetPdgArray.length);
                System.arraycopy(changedPdgArray, 0, combinePdgArray, targetPdgArray.length, changedPdgArray.length);
                hashCalculate(combinePdgArray,NUMBER_OF_THREADS,mappingPDGToPDGNodes,mappingPDGToPDGEdges);
                final SortedSet<ClonePairInfo> clonepairs = Collections
                        .synchronizedSortedSet(new TreeSet<ClonePairInfo>());
                System.out.println("round: " + compareRound);
                detectClonePair(targetPdgArray,changedPdgArray,NUMBER_OF_THREADS,SIZE_THRESHOLD,
                        mappingPDGToPDGNodes,mappingPDGToPDGEdges,clonepairs);//calculate similar degree at the same time
                System.out.println("writing to a file ... ");
                BufferedWriter writer1 = new BufferedWriter(new FileWriter(output,true));
                writer1.write("start round\t");
                writer1.write(Integer.toString(compareRound));
                writer1.write("\tcommit\t");
                writer1.write(record.commitID);
                writer1.write("\n");
                writer1.close();
                final Writer writer = new BellonWriter(output, clonepairs);
                writer.write();
                System.out.println("done: ");

                final File file = new File("./" + projectToDetect);
                if (!file.exists()) {
                    System.err.println("specified directory or file does not exist.");
                    System.exit(0);
                }
                PDG[] remainedPdgArray;
                final SortedSet<PDG> pdgs1 = Collections
                        .synchronizedSortedSet(new TreeSet<PDG>());
                for(PDG pdg : targetPdgArray){  //find unchanged pdgs
                    boolean find = false;
                    for(File changedfile : changedfiles){
                        if(changedfile.getAbsolutePath().equals(pdg.unit.path)){
                            find = true;
                            break;
                        }
                    }
                    if(!find){   //remain pdgs unchanged and no matter if it still exists in current branch(for solve merge bug)
//                        File temp = new File(pdg.unit.path);
//                        if(temp.exists()){
//                            String actual = temp.getCanonicalFile().getName();
//                            String recordname = pdg.unit.path.substring(pdg.unit.path.lastIndexOf('\\') == -1 ? 0 : pdg.unit.path.lastIndexOf('\\') + 1);
//                            if(actual.equals(recordname)){
//                                pdgs1.add(pdg);
//                            }
//                        }
//                        else{
                        pdgs1.add(pdg);
//                        }
                    }

                }
                remainedPdgArray = pdgs1.toArray(new PDG[0]);
                targetPdgArray = new PDG[remainedPdgArray.length + changedPdgArray.length];
                System.arraycopy(remainedPdgArray,0,targetPdgArray,0,remainedPdgArray.length);
                System.arraycopy(changedPdgArray, 0, targetPdgArray, remainedPdgArray.length, changedPdgArray.length);
                commitStack.pop();
                writeRecord(pdgrecord,targetPdgArray,record.commitID);
                compareRound++;
            }
            final long time5 = System.nanoTime();
            System.out.println("total elapsed time: ");
            printTime(time5 - time1);

            System.out.println("number of comparisons: ");
            printNumberOfComparison(Slicing.getNumberOfComparison());

            System.out.println("write author result");
            if(targetPdgArray.length == 0){
                System.out.println("00000");
            }
            for(PDG pdg : targetPdgArray) {
                File temp = new File(pdg.unit.path);
                if (temp.exists()) {
                    String fp = pdg.unit.path.substring(pdg.unit.path.indexOf(projectToDetect) + projectToDetect.length() + 1);

                    fp = fp.replaceAll("\\\\", "\\/");
                    String storepath = projectToDetect + "/gitAuthorResult/" + fp.substring(0, fp.indexOf(".java")) + "A.txt";
                    String blamepath = projectToDetect + "/gitBlameResult/" + fp.substring(0, fp.indexOf(".java")) + "B.txt";
                    File f1 = new File(storepath);
                    File f2 = new File(blamepath);
                    if (!f1.exists() || !f2.exists()) {
                        if (!f1.getParentFile().exists()) {
                            f1.getParentFile().mkdirs();
                        }
                        if (!f2.getParentFile().exists()) {
                            f2.getParentFile().mkdirs();
                        }
                        String[] cmd1 = {"sh", "-c", "\"cd " + projectToDetect + " && git blame " + fp + " > gitBlameResult/" + fp.substring(0, fp.indexOf(".java")) + "B.txt\""};
                        Process p2 = Runtime.getRuntime().exec(cmd1);
                        p2.waitFor();
                        RandomAccessFile ra = new RandomAccessFile(blamepath, "rw");// (file name, mode of file)
                        StringBuffer buf = new StringBuffer();
                        String line;
                        int currentLine = 1;

                        while ((line = ra.readLine()) != null) {
                            if (currentLine >= pdg.unit.startLine && currentLine <= pdg.unit.endLine) {
                                buf.append(pdg.unit.authorName + "\t");
                                buf.append(line.substring(line.indexOf(')') - 4).trim() + "\r\n"); //can only handle code files within 999 lines
                            } else {
                                buf.append(line + "\r\n");
                            }
                            currentLine++;
                        }
                        ra.close();
                        writeFile(storepath, buf.toString());
                    } else {
                        RandomAccessFile ra = new RandomAccessFile(storepath, "rw");// (file name, mode of file)
                        StringBuffer buf = new StringBuffer();
                        String line;
                        int currentLine = 1;

                        while ((line = ra.readLine()) != null) {
                            if (currentLine >= pdg.unit.startLine && currentLine <= pdg.unit.endLine) {
                                buf.append(pdg.unit.authorName + "\t");
                                buf.append(line.substring(line.indexOf(')') - 4).trim() + "\r\n"); //can only handle code files within 999 lines
                            } else {
                                buf.append(line + "\r\n");
                            }
                            currentLine++;
                        }
                        ra.close();
                        writeFile(storepath, buf.toString());
                    }
                }
            }
                System.out.println("Done.");

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
    }



    private static List<File> getFiles(final File file) {

        final List<File> files = new ArrayList<File>();

        if (file.isFile()) {
            if (file.getName().endsWith(".java")) {
                files.add(file);
            }
        }

        else if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                files.addAll(getFiles(child));
            }
        }

        else {
            assert false : "\"file\" is invalid.";
        }

        return files;
    }

    private static void writeRecord(String file, PDG[] pdgs, String commitID) throws IOException {
        BufferedWriter writer1 = new BufferedWriter(new FileWriter(file,true));
        writer1.write("commit\t");
        writer1.write(commitID);
        writer1.write("\n");
        for(PDG pdg : pdgs){
            writer1.write(pdg.unit.path + "\t" + pdg.unit.name + "\t" + pdg.unit.authorName + "\n");
        }
        writer1.close();
    }

    private static void generatePDGs(String author,List<File> files,final SortedSet<PDG> pdgs, boolean useOfControl, boolean useOfData,boolean useOfExecution,
                                     boolean useOfMerging, int SIZE_THRESHOLD, int NUMBER_OF_THREADS){

        final List<MethodInfo> methods = new ArrayList<MethodInfo>();
        for (final File file : files) {
            final CompilationUnit unit = TinyPDGASTVisitor
                    .createAST(file);
            final TinyPDGASTVisitor visitor = new TinyPDGASTVisitor(
                    file.getAbsolutePath(), unit, methods);
            unit.accept(visitor);
        }

        final CFGNodeFactory cfgNodeFactory = new CFGNodeFactory();
        final PDGNodeFactory pdgNodeFactory = new PDGNodeFactory();
        final Thread[] pdgGenerationThreads = new Thread[NUMBER_OF_THREADS];
        for (int i = 0; i < pdgGenerationThreads.length; i++) {
            pdgGenerationThreads[i] = new Thread(
                    new PDGGenerationThread(methods, pdgs,
                            cfgNodeFactory, pdgNodeFactory,
                            useOfControl, useOfData, useOfExecution,
                            useOfMerging, SIZE_THRESHOLD,author));
            pdgGenerationThreads[i].start();
        }
        for (final Thread thread : pdgGenerationThreads) {
            try {
                thread.join();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void hashCalculate(PDG[] combinePdgArray,int NUMBER_OF_THREADS,
                                      SortedMap<PDG, SortedMap<PDGNode<?>, Integer>> mappingPDGToPDGNodes,
                                      SortedMap<PDG, SortedMap<PDGEdge, Integer>> mappingPDGToPDGEdges){
        System.out.print("calculating hash values of target directory and changed function ... ");
        final Thread[] hashCalculationThreads = new Thread[NUMBER_OF_THREADS];
        for (int i = 0; i < hashCalculationThreads.length; i++) {
            hashCalculationThreads[i] = new Thread(
                    new HashCalculationThread(combinePdgArray,
                            mappingPDGToPDGNodes, mappingPDGToPDGEdges));
            hashCalculationThreads[i].start();//turn to hash value
        }
        for (final Thread thread : hashCalculationThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.print("done.");
    }

    private static void writeFile(String path,String content) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        bw.write(content);
        bw.close();
    }

    private static void detectClonePair(PDG[] targetPdgArray,PDG[] changedPdgArray,int NUMBER_OF_THREADS,int SIZE_THRESHOLD,
                                        SortedMap<PDG, SortedMap<PDGNode<?>, Integer>> mappingPDGToPDGNodes,
                                        SortedMap<PDG, SortedMap<PDGEdge, Integer>> mappingPDGToPDGEdges,
                                        SortedSet<ClonePairInfo> clonepairs){
        System.out.print("detecting clone pairs ... ");

        {
            final List<PDGPairInfo> pdgpairs = new ArrayList<PDGPairInfo>();
            for(int i = 0;i < changedPdgArray.length;i++) {
                for (int j = 0; j < targetPdgArray.length; j++) {
                    pdgpairs.add(new PDGPairInfo(changedPdgArray[i], targetPdgArray[j]));//to do: compare with the same file first,if no match, compare with whole repository
                }
            }
            final PDGPairInfo[] pdgpairArray = pdgpairs
                    .toArray(new PDGPairInfo[0]);
            final Thread[] slicingThreads = new Thread[NUMBER_OF_THREADS];
            for (int i = 0; i < slicingThreads.length; i++) {
                slicingThreads[i] = new Thread(new SlicingThread(
                        pdgpairArray, mappingPDGToPDGNodes,
                        mappingPDGToPDGEdges, clonepairs, SIZE_THRESHOLD));
                slicingThreads[i].start();//生成clone pair
            }
            for (final Thread thread : slicingThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        Map<String, String> methodNameMap = new HashMap<String, String>();
        for(ClonePairInfo cp : clonepairs){//keep old author
            //if contains methodA; if previous pathA == pathB; if previous methodA == methodB; if current methodA == methodB;if current methodA == methodB
            String fullMethodA = cp.pathA + cp.methodA;
            String fullMethodB = cp.pathB + cp.methodB;
            if(fullMethodB.equals(fullMethodA)){
                methodNameMap.put(fullMethodA,fullMethodB);
                changedPdgArray[cp.compareNumber/ targetPdgArray.length].unit.authorName = targetPdgArray[cp.compareNumber % targetPdgArray.length].unit.authorName;
            }
            else if(methodNameMap.containsKey(fullMethodA)){
                if(methodNameMap.get(fullMethodA).equals(fullMethodA)){
                    continue;
                }
                else if(cp.pathB.equals(cp.pathA) || cp.methodA == cp.methodB){
                    methodNameMap.put(fullMethodA,fullMethodB);
                    changedPdgArray[cp.compareNumber/ targetPdgArray.length].unit.authorName = targetPdgArray[cp.compareNumber % targetPdgArray.length].unit.authorName;
                }
                else{
                    continue;
                }
            }
            else{
                methodNameMap.put(fullMethodA,fullMethodB);
                changedPdgArray[cp.compareNumber/ targetPdgArray.length].unit.authorName = targetPdgArray[cp.compareNumber % targetPdgArray.length].unit.authorName;
            }
        }
        System.out.print("done: ");
    }

    private static void getCommitRecord(Stack<CommitRecord> commitStack,String filePath) throws IOException, InterruptedException {
        try {
            String[] cmd = {"sh", "-c", "\"cd " + projectToDetect + " && git log --name-only > commit.txt\""};
            Process p2 = Runtime.getRuntime().exec(cmd);
            p2.waitFor();

            //BufferedReader是可以按行读取
            FileInputStream inputStream = new FileInputStream(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String str = null;
            String commitID = null;
            String Author = null;
            String[] filePaths;
            List<String> filepaths = new ArrayList<>();
            while ((str = bufferedReader.readLine()) != null) {
                if (str.startsWith("commit ")) {
                    if (!filepaths.isEmpty()) {
                        filePaths = filepaths.toArray(new String[0]);
                        commitStack.push(new CommitRecord(commitID, Author, filePaths));
                        //Author = null;
                        filepaths.clear();
                    }
                    commitID = str.substring(7);
                } else if (str.startsWith("Author:")) {
                    Author = str.substring(8, str.indexOf('<') - 1);
                } else if (!str.isEmpty() && !str.startsWith(" ") && !str.startsWith("Date:")) {
                    if (str.endsWith(".java")) {
                        filepaths.add(str);
                    }
                }
            }
            if (!filepaths.isEmpty()) {
                filePaths = filepaths.toArray(new String[0]);
                commitStack.push(new CommitRecord(commitID, Author, filePaths));
                filepaths.clear();
            }
            //close
            inputStream.close();
            bufferedReader.close();
        }catch(IOException e){
            System.err.println(e.getMessage());
            System.exit(0);
        }
    }

    private static void printNumberOfRemoval(final long number) {
        System.out.print("number of removed edges: ");
        System.out.println(String.format("%1$,3d", number));
    }

    private static void printNumberOfComparison(final long number) {
        System.out.println(String.format("%1$,3d", number));
    }

    private static void printTime(final long time) {
        final long micro = time / 1000;
        final long mili = micro / 1000;
        final long sec = mili / 1000;

        final long hour = sec / 3600;
        final long minute = (sec % 3600) / 60;
        final long second = (sec % 3600) % 60;

        if (1l == hour) {
            System.out.print(hour);
            System.out.print(" hour ");
        } else if (1l < hour) {
            System.out.print(hour);
            System.out.print(" hours ");
        }

        if (1l == minute) {
            System.out.print(minute);
            System.out.print(" minute ");
        } else if (1l < minute) {
            System.out.print(minute);
            System.out.print(" minutes ");
        } else if ((0l == minute) && (1l <= hour)) {
            System.out.print(" 0 minute ");
        }

        if (2 <= second) {
            System.out.print(second);
            System.out.println(" seconds.");
        } else {
            System.out.print(second);
            System.out.println(" second.");
        }
    }
}