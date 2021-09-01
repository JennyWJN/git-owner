package yoshikihigo.tinypdg.scorpio;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class comparison {
    public static String projectToDetect = "main";
    public static String output = "./" + projectToDetect + "/compare1.txt";
    public static long totalline = 0;
    public static long methodline = 0;
    public static void main(String[] args) {
        File AutResult = new File("./" + projectToDetect + "/gitAuthorResult");
        try {
            List<File> Aut = getFiles(AutResult);
            for (final File aut : Aut) {
                String absolutePath = aut.getCanonicalPath();
                String relativePath = absolutePath.substring(absolutePath.indexOf("gitAuthorResult") + 15);
                String bla = "D:\\master\\dissertation\\gitOwner\\" + projectToDetect + "\\gitBlameResult" + relativePath.substring(0,relativePath.length()-5) + "B.txt";
                matchTwoFile(absolutePath,bla);
            }
            System.out.println("total line number: " + totalline);
            System.out.println("detected method line number: " + methodline);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
    }

    private static List<File> getFiles(final File file) {

        final List<File> files = new ArrayList<File>();

        if (file.isFile()) {
            if (file.getName().endsWith(".txt")) {
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

    private static void matchTwoFile(String aut, String bla) throws IOException {
        RandomAccessFile ra = new RandomAccessFile(aut,"rw" );// (file name, mode of file)
        RandomAccessFile rb = new RandomAccessFile(bla,"rw");
        String lineA;
        String lineB;
        StringBuffer sameLines = new StringBuffer();
        StringBuffer differentLines = new StringBuffer();
        int currentLine = 1;
        int startline = 1;
        int scount = 0;
        int dcount = 0;
        boolean isSame = true;
        while(((lineA = ra.readLine()) != null) && ((lineB = rb.readLine()) != null)){
            totalline++;
            if(lineA.indexOf('(') != -1 && lineA.indexOf('(') < lineA.indexOf(')')){ //use blame result
                if(!isSame){
                    isSame = true;
                    differentLines.append(startline + "-" + Integer.toString(currentLine-1) + "\t");
                    startline = currentLine;
                }
            }
            else{  //use aut result
                methodline++;
                lineB = lineB.substring(lineB.indexOf('(') + 1);
                String blaAuthor = lineB.substring(0,lineB.indexOf(')') - 29).trim();
                lineA = lineA.substring(0,lineA.indexOf(')'));
                String autAuthor = lineA.substring(0,lineA.lastIndexOf('\t'));
                if(blaAuthor.equals(autAuthor)){
                    if(!isSame){
                        isSame = true;
                        differentLines.append(startline + "-" + (currentLine-1) + "\t");
                        startline = currentLine;
                    }
                }
                else{
                    if(isSame){
                        isSame = false;
                        if(currentLine == 1) continue;
                        sameLines.append(startline + "-" + (currentLine-1) + "\t");
                        startline= currentLine;
                    }
                }
            }
            if(isSame){
                scount++;
            }
            else{
                dcount++;
            }
            currentLine++;
        }
        ra.close();
        rb.close();
        if(currentLine != 1){
            double rate = new BigDecimal((float)dcount/(currentLine-1)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            if(rate > 0.05){
                BufferedWriter bw = new BufferedWriter(new FileWriter(output,true));
                bw.write(aut.substring(aut.indexOf("gitAuthorResult") + 16,aut.length()-5));
                bw.write("\r\ntotal lines number: " + (currentLine-1));
                bw.write("\r\nsame lines: ");
                bw.write(String.valueOf(sameLines));
                bw.write("\r\nsame lines number: " + scount);
                bw.write("\r\ndifferent lines: ");
                bw.write(String.valueOf(differentLines));
                bw.write("\r\ndifferent lines number: " + dcount);
                bw.write("\r\ndifferent percentage: " + rate);
                bw.write("\r\n");
                bw.close();
            }
        }

    }
}
