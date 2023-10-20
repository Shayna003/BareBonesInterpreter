package bareBones;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This program is a interpreter for Bare Bones language proposed by Brookshear in his book Computer Science: an Overview.
 * indents and line breaks are not necessary for correct execution. Only semicolons are used to separate statements apart.
 * Naming variables should use word characters in the range [a-zA-Z0-9_]
 *
 * To use command line version, run: java bareBones.BareBonesInterpreter
 * To print how each statement is evaluated, run: java bareBones.BareBonesInterpreter verbose
 * To launch GUI version, run: java bareBones.BareBonesInterpreter GUI
 * To use input redirect, run: java bareBones.BareBonesInterpreter < input.txt
 * To use output redirect with GUI, run: java bareBones.BareBonesInterpreter GUI > output.txt
 *
 * valid uses cases:
 * java bareBones.BareBonesInterpreter verbose
 *
 * java bareBones.BareBonesInterpreter verbose
 *
 * java bareBones.BareBonesInterpreter < input.txt
 *
 * java bareBones.BareBonesInterpreter verbose < input.txt
 *
 * java bareBones.BareBonesInterpreter < input.txt > output.
 *
 * java bareBones.BareBonesInterpreter < input.txt > output.txt verbose
 *
 * java bareBones.BareBonesInterpreter GUI
 *
 * java bareBones.BareBonesInterpreter GUI > output.txt
 *
 * java bareBones.BareBonesInterpreter GUI > output.txt verbose
 *
 * TODO: output for commandline mode is messed up
 * TODO: indent support, special highlighting, popup menu stuff, such as insert commands, auto-completion, memory use
 * TODO: line numbers
 */
public class BareBonesInterpreter
{
    /**
     * A language keyword
     */
    enum Keyword
    {
        CLEAR("clear"), INCR("incr"), DECR("decr"), WHILE("while"), NOT("not"), DO("do"), END("end");
        private final String str;
        Keyword(String str)
        {
            this.str = str;
        }
    }

    /**
     * Represents a while loop, essentially maintained by pushing and popping a stack
     */
    class Loop
    {
        String counter;
        int bound;
        int lineNumber;

        @Override
        public String toString()
        {
            return "loop{counter=" + counter + ",bound=" + bound + ",lineNumber=" + (lineNumber+1) + "}";
        }
        public Loop(String counter, int bound, int lineNumber)
        {
            this.counter = counter;
            this.bound = bound;
            this.lineNumber = lineNumber;
        }

        public boolean valid()
        {
            return memory.getOrDefault(counter, 0) != bound;
        }
    }
    volatile Map<String, Integer> memory; // hashmap of all variables
    volatile Stack<Loop> loops; // stack of all current loops in effect

    void clearMemory()
    {
        memory.clear();
        loops.clear();
        //System.out.println(memory);
        //System.out.println(loops);
    }

    public BareBonesInterpreter()
    {
        verbose = false;
        memory = new HashMap<>();
        loops = new Stack<>();
        output = new StringBuilder();
    }

    boolean commandline = false;
    StringBuilder outputString = new StringBuilder();
    /**
     * Print all variables stored in memory
     * @param out output stream
     */
    void printVariables(PrintStream out)
    {
        //System.out.println("print variables");
        //System.out.println("memory: " + memory);
        memory.forEach((k, v) ->
                {
                    if (out != null)
                    {
                        if (verbose) out.print("    ");
                        out.println(k + "= " + v);
                    }
                    if (gui != null || commandline)
                    {
                        if (verbose) outputString.append("    ");
                        outputString.append(k);
                        outputString.append("= ");
                        outputString.append(v);
                        outputString.append(System.lineSeparator());
                    }
                });
        if (gui != null && loops.size() > 1 && verbose)
        {
            outputString.append("    ");
            outputString.append("loops: ");
            outputString.append(System.lineSeparator());
        }
        loops.forEach(loop ->
        {
            if (out != null)
            {
                if (verbose) out.print("    ");
                out.print(loop);
                out.println();
            }
            if (gui != null)
            {
                outputString.append("    " + loop);
                outputString.append(System.lineSeparator());
            }
        });
        if (verbose) System.out.println();
        if (gui != null)
        {
            //outputString.append(System.lineSeparator());
            gui.output.setText(outputString.toString());
        }
    }

    StringBuilder output; // used for GUI

    boolean verbose;
    /**
     * Processes a line of input and appends output to the output StringBuilder
     * @param input a line of String input
     * @return the line number to go to after this statement
     */
    int runStatement(String input, int lineNumber, int totalLines)
    {
        if (verbose)
        {
            System.out.println("processing statement \"" + input + "\" at line " + (lineNumber+1) + ": ");
            outputString.append("processing statement \"" + input + "\" at line " + (lineNumber+1) + ": ");
            outputString.append(System.lineSeparator());
            //outputString.append(System.lineSeparator());
        }
        //if (this.gui != null)
        String[] tokens = Pattern.compile("(\\W)++").split(input);
        if (tokens == null || tokens.length == 0) return lineNumber + 1;
        Keyword keyword = null;
        int index = -1;
        for (int i = 0; i < tokens.length; i++)
        {
            keyword = detectKeyword(tokens[i]);
            if (keyword != null)
            {
                index = i;
                break;
            }
        }
        if (keyword == null) return lineNumber + 1;
        if (keyword == Keyword.INCR)
        {
            memory.put(tokens[index + 1], memory.getOrDefault(tokens[index + 1], 0) + 1);
        }
        else if (keyword == Keyword.DECR)
        {
            memory.put(tokens[index + 1], memory.getOrDefault(tokens[index + 1], 0) - 1);
        }
        else if (keyword == Keyword.CLEAR)
        {
            memory.remove(tokens[index + 1]);
        }
        else if (keyword == Keyword.WHILE) // while x not y do;
        {
            memory.putIfAbsent(tokens[index + 1], 0);
            Loop loop = new Loop(tokens[index + 1], Integer.parseInt(tokens[index + 3]), lineNumber);
            loops.push(loop);
        }
        else if (keyword == Keyword.END)
        {
            Loop loop = loops.peek();
            if (loop.valid())
            {
                if (verbose)
                {
                    System.out.println("go to line number " + (loop.lineNumber + 1));
                    outputString.append("go to line number " + (loop.lineNumber + 1));
                    outputString.append(System.lineSeparator());
                }
                lineNumber = loop.lineNumber;
            }
            else
            {
                loops.pop();
                //System.out.println("stack.pop: " + loop);
            }
        }
        if (verbose || lineNumber + 1 >= totalLines) printVariables(System.out);
        return lineNumber + 1;
    }

    GUI gui;
    Keyword detectKeyword(String str)
    {
        for (Keyword keyword : Keyword.values())
        {
            if (str.equals(keyword.str)) return keyword;
        }
        return null;
    }
    /**
     * Interpret all input instructions using current memory
     * @param input string instructions
     * @return string output
     */
    String interpret(String input)
    {
        String[] lines = input.split("(\\PL)*;(\\PL)*");
        for (int i = 0; i < lines.length;)
        {
            int line = runStatement(lines[i], i, lines.length);
            i = line;
        }
        ///System.out.println("Finished Processing!");
        //System.out.println(loops);
        return output.toString();
    }

    /**
     * Main method, should be invoked for this package
     * @param args list of arguments, valid ones are verbose and gui"
     */
    public static void main(String[] args)
    {
        //System.out.println(Arrays.toString(args));
        boolean gui = false;
        BareBonesInterpreter interpreter = new BareBonesInterpreter();
        if (args != null)
        {
            for (String arg : args)
            {
                if (arg.equalsIgnoreCase("GUI"))
                {
                    gui = true;
                }
                else if (arg.equalsIgnoreCase("verbose"))
                {
                    interpreter.verbose = true;
                }
            }
        }

        if (gui) // launch Graphical User Interface
        {
            EventQueue.invokeLater(() ->
            {
                interpreter.gui = new GUI(interpreter);
                interpreter.gui.setVisible(true);
            });
        }
        else
        {
            Scanner scanner = new Scanner(System.in);
            String statement;
            scanner.useDelimiter("(\\PL)*;(\\PL)*");

            int line = 0;
            ArrayList<String> statements = new ArrayList<>();
            if (System.console() == null) // redirection detected
            {
                while (scanner.hasNext() || line < statements.size())
                {
                    if (statements.isEmpty() || line >= statements.size())
                    {
                        statement = scanner.next();
                        statements.add(statement);
                    }
                    else
                    {
                        statement = statements.get(line);
                    }
                    line = interpreter.runStatement(statement, line, Integer.MAX_VALUE);
                }
                if (interpreter.verbose)
                {
                    interpreter.verbose = false;
                    System.out.println("final results:");
                }
                interpreter.printVariables(System.out);
            }
            else
            {
                /**
                 * Just like command line python,
                 * Math expressions are directly evaluated and printed back (not implemented yet)
                 * variable assignments are global
                 * while loops wait for a matching end clause to evaluate
                 */
                System.out.println("Welcome to the bare bones interpreter!");
                System.out.println("There is a GUI version too! launch with \"java bareBones.BareBonesInterpreter GUI\"");
                System.out.println("To quit at any time, type \"quit;\"");
                System.out.println("Enter a command to see immediate input:");
                boolean quit = false;

                while (!quit)
                {
                    System.out.print(System.lineSeparator() + "  >>");
                    if (statements.isEmpty() || line >= statements.size())
                    {
                        statement = scanner.next();
                        statements.add(statement);
                    }
                    else
                    {
                        statement = statements.get(line);
                    }
                    line = interpreter.runStatement(statement, line, Integer.MAX_VALUE);
                    if (!interpreter.verbose)
                    {
                        System.out.print("    ");
                        interpreter.printVariables(System.out);
                    }

                    if (statement.contains("quit"))
                    {
                        quit = true;
                    }
                }
                System.out.println("Thank you for using bare bones interpreter! I recommend using the GUI version.");
            }
        }
    }
}

/**
 * GUI support, not necessary for the program to function
 */
class GUI extends JFrame
{
    BareBonesInterpreter interpreter;
    JFileChooser fileChooser;
    JMenuItem openItem;
    JMenuItem openAnRunItem;
    JMenu openMenu;
    JMenuBar menuBar;
    JTextArea input;
    JTextArea output;
    JButton run;
    JCheckBox verbose;
    JButton printMemory;
    JButton clear;
    JButton clearInput;
    JButton clearOutput;

    JPanel controlPanel;
    JSplitPane splitPane;

    String getStringFromFile(File file) throws IOException
    {
        if (file == null || !file.exists()) return null;
        return new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8);
    }

    File selectFile()
    {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            return fileChooser.getSelectedFile();
        }
        return  null;
    }

    public GUI(BareBonesInterpreter interpreter)
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.interpreter = interpreter;
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        fileChooser.setDragEnabled(true);
        openItem = new JMenuItem("File...");
        openItem.addActionListener(event ->
        {
            try
            {
                String str = getStringFromFile(selectFile());
                input.setText(str);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred, type: " + e.getClass(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        openAnRunItem = new JMenuItem("File And Run...");
        openAnRunItem.addActionListener(event ->
        {
            try
            {
                String str = getStringFromFile(selectFile());
                input.setText(str);
                interpreter.interpret(str);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred, type: " + e.getClass(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        openMenu = new JMenu("Import");
        openMenu.add(openItem);
        openMenu.add(openAnRunItem);
        menuBar = new JMenuBar();
        menuBar.add(openMenu);
        setJMenuBar(menuBar);

        controlPanel = new JPanel();

        run = new JButton("Run");
        run.addActionListener(event ->
        {
            interpreter.outputString.delete(0, interpreter.outputString.length());
            interpreter.interpret(input.getText());
        });
        controlPanel.add(run);

        clear = new JButton("Clear Memory");
        clear.addActionListener(event ->
        {
            interpreter.clearMemory();
            interpreter.outputString.delete(0, interpreter.outputString.length());
            output.setText("");
        });
        controlPanel.add(clear);

        printMemory = new JButton("Print Memory");
        printMemory.addActionListener(event ->
        {
            interpreter.outputString.delete(0, interpreter.outputString.length());
            interpreter.printVariables(null);
        });
        controlPanel.add(printMemory);

        clearInput = new JButton("Clear Input");
        clearInput.addActionListener(event ->
        {
            input.setText("");
        });
        controlPanel.add(clearInput);

        clearOutput = new JButton("Clear Output");
        clearOutput.addActionListener(event ->
        {
            output.setText("");
        });
        controlPanel.add(clearOutput);

        verbose = new JCheckBox("Verbose Output");
        verbose.setSelected(interpreter.verbose);
        verbose.addActionListener(event ->
        {
            interpreter.verbose = verbose.isSelected();
        });
        controlPanel.add(verbose);

        input = new JTextArea(20, 20);
        input.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), "Input"));
        output = new JTextArea(20, 20);
        output.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), "Output"));
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(input), new JScrollPane(output));
        splitPane.setDividerLocation(0.5);

        add(splitPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setTitle("Welcome to Bare Bones Interpreter!");
        pack();
        setLocationRelativeTo(null);
    }
}