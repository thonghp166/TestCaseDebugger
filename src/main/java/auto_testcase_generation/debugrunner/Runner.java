package auto_testcase_generation.debugrunner;

import com.dse.compiler.AvailableCompiler;
import com.dse.compiler.Compiler;
import com.dse.compiler.Terminal;
import com.dse.guifx_v3.helps.Environment;
import com.dse.util.CompilerUtils;
import com.dse.util.IGTestConstant;

import java.io.*;

public class Runner {
    private String testdriver;

    public static void main(String[] args) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        // generate exe
        String testdriver = "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/d/test-drivers/test0_0.6968817.cpp";
        String out = "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/d/test-drivers/test0_0.6968817.exe";
        String compilationCommand = String.format("g++ -std=c++11 \"%s\" -o \"%s\" %s", testdriver, out, IGTestConstant.COMPILE_FLAG_FOR_GOOGLETEST);
        System.out.println(compilationCommand);

        Environment.getInstance().setCompiler(new Compiler(AvailableCompiler.CPP_11_GNU_NATIVE.class));
        String[] script = CompilerUtils.prepareForTerminal(Environment.getInstance().getCompiler(), compilationCommand);

        String response = new Terminal(script).get();
        System.out.println("Response = " + response);

        // -----
        Runner runner = new Runner();
        runner.debug(out);
    }

    public void debug(String out) throws IOException {
        // Start gdb with mi interpreter
        String executableFile = new File(out).getCanonicalPath();
        String gdbcommand = String.format("%s %s %s", PATH_GDB, executableFile, RUN_WITH_GDB_MI);
        System.out.println("Debug Command: " + gdbcommand);
        Process gdbProcess = Runtime.getRuntime().exec(gdbcommand);

        if (gdbProcess != null) {
//            runCommandInGdb(gdbProcess, "-break-insert main", false);
            runCommandInGdb(gdbProcess, "-break-insert main", false);
            runCommandInGdb(gdbProcess, String.format("-break-insert \"%s:%s\"",
                    "/Users/ducanhnguyen/Documents/akautauto/local/working-directory/d/test-drivers/test0_0.6968817.cpp",
                    55
            ), false);
            runCommandInGdb(gdbProcess, "-gdb-exit", true);

            // A remaining bug: can not display information on the debugger during the execution!
            // In the current version, I have to put the displaying statement to the end of debugging exeuctions.
            displayResultinTerminal(gdbProcess);

            gdbProcess.destroy();
        }
    }

    /**
     * read the result of command from the terminal
     */
    public static void displayResultinTerminal(Process gdbProcess) throws IOException {
        System.out.println("Getting the result from the terminal");
        BufferedReader in = new BufferedReader(new InputStreamReader(gdbProcess.getInputStream()));
        String line = "";
        while ((line = in.readLine()) != null) {
            System.out.println("\t" + line);
        }
    }

    /**
     * Add breakpoint to main function
     *
     * @param process
     */
    public void runCommandInGdb(Process process, String command, boolean shouldClosed) {
        // execute gdb command
        System.out.println("Command " + command);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())), true);
        out.println(command);
        out.flush();
        if (shouldClosed) {
            out.close();
        }
    }

    public Runner() {

    }

    public String getTestdriver() {
        return testdriver;
    }

    public void setTestdriver(String testdriver) {
        this.testdriver = testdriver;
    }

    public static final String PATH_GDB = "/usr/local/bin/gdb";
    public static final String RUN_WITH_GDB_MI = "--interpreter=mi";

}
