package com.dse.compiler;

import com.dse.util.SpecialCharacter;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Terminal {
    private String stdOut = SpecialCharacter.EMPTY;

    private String stdErr = SpecialCharacter.EMPTY;

    private Process process;

    public Terminal(String script) throws IOException, InterruptedException {
        exec(script);
    }

    public Terminal(String[] script) throws IOException, InterruptedException {
        exec(null, script);
    }

    public Terminal(String[] script, String directory) throws IOException, InterruptedException {
        exec(directory, script);
    }

    public Terminal(String script, String directory) throws IOException, InterruptedException {
        exec(directory, script);
    }

    private void exec(String script) throws IOException, InterruptedException {
        exec(null, script);
    }

    private void exec(String directory, String... script) throws IOException, InterruptedException {
        for (int i = 0; i < script.length; i++)
            script[i] = script[i].trim();

        if (directory != null) {
            if (script.length == 1)
                process = Runtime.getRuntime().exec(script[0], null, new File(directory));
            else
                process = Runtime.getRuntime().exec(script, null, new File(directory));
        } else {
            if (script.length == 1)
                process = Runtime.getRuntime().exec(script[0]);
            else
                process = Runtime.getRuntime().exec(script);
        }

        // If you do not use waitFor, the linking command line
        // is not able to execute on MAC-OSX
//        process.waitFor(5, TimeUnit.SECONDS);

        StreamReader inputStreamReader = new StreamReader(process.getInputStream());
        StreamReader errorStreamReader = new StreamReader(process.getErrorStream());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            executorService.invokeAll(Arrays.asList(errorStreamReader, inputStreamReader));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService.shutdown();

        process.waitFor();

        stdErr = errorStreamReader.getResponse();
        stdOut = inputStreamReader.getResponse();
    }

    public Process getProcess() {
        return process;
    }

    public String get() {
        return stdErr + "\n" + stdOut;
    }

    public String getStderr() {
        return stdErr;
    }

    public String getStdout() {
        return stdOut;
    }

    public class StreamReader implements Callable<String> {
        private InputStream is;
        private String response = SpecialCharacter.EMPTY;

        public StreamReader(InputStream is) {
            this.is = is;
        }

        @Override
        public String call() throws Exception {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                while (true) {
                    String s = br.readLine();

                    if (s == null)
                        break;

                    response += s + SpecialCharacter.LINE_BREAK;

//                    System.out.println(String.format("[%s] %s", is.getClass().getSimpleName(), s));
                }

                is.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return response;
        }

        public String getResponse() {
            return response;
        }
    }
}
