package auto_testcase_generation.testdata.object;

import com.dse.compiler.AvailableCompiler;
import com.dse.compiler.Compiler;
import com.dse.compiler.Terminal;
import com.dse.debugger.gdb.IGDBMI;
import com.dse.guifx_v3.helps.Environment;
import com.dse.util.CompilerUtils;
import com.dse.util.IGTestConstant;
import com.dse.util.Utils;

import java.io.*;

public class SE2 extends SE{

    public static void main(String[] args) throws InterruptedException, IOException, NoSuchFieldException, IllegalAccessException {
        SE2 myGdbmi = new SE2();

        myGdbmi.setTestdriver("/Users/ducanhnguyen/Documents/akautauto/local/working-directory/algo/test-drivers/reverse_array.71713.cpp");
        myGdbmi.setSrc2("/Users/ducanhnguyen/Documents/akautauto/datatest/duc-anh/Algorithm/algo.Utils.akaignore.cpp");
        myGdbmi.setExe("/Users/ducanhnguyen/Documents/akautauto/local/working-directory/algo/test-drivers/reverse_array.71713.exe");

        myGdbmi.compile();
        myGdbmi.debug(myGdbmi.getExe());
    }

    @Override
    public void debug(String out) throws IOException {
        try {
            String gdbcommand = "/data/bin/gdb " + getExe();
            String line;

            Process process = Runtime.getRuntime().exec("su");

            if (process != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                PrintWriter outPrinter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())),true);

                outPrinter.println(gdbcommand);

                //Note this line does not get sent to gdb's interface after starting gdb.
                //Is it possible to connect to the new stdout of gdb's interface?
                outPrinter.println("info registers");

                outPrinter.flush();
                outPrinter.close();

                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }

                process.destroy();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
