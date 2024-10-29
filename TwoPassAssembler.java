import javax.swing.*;
import java.awt.event.*;
import java.util.HashMap;

public class TwoPassAssembler {
    private JFrame frame;
    private JTextArea inputArea, optabArea, outputArea;
    private JButton passOneButton, passTwoButton;
    private HashMap<String, String> symtab = new HashMap<>();
    private HashMap<String, String> optab = new HashMap<>();
    private StringBuilder intermediate = new StringBuilder();
    private StringBuilder symtabOutput = new StringBuilder();
    private int finalLocctr = 0;  

    public static void main(String[] args) {
        new TwoPassAssembler().initialize();
    }

    public void initialize() {
        frame = new JFrame("Two-Pass Assembler");
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);

        frame.setVisible(true);
    }

    private void placeComponents(JPanel panel) {
        panel.setLayout(null);

        
        JLabel optabLabel = new JLabel("OPTAB:");
        optabLabel.setBounds(10, 10, 80, 25);
        panel.add(optabLabel);

        optabArea = new JTextArea();
        optabArea.setBounds(100, 10, 210, 150);
        panel.add(optabArea);

        
        JLabel inputLabel = new JLabel("INPUT:");
        inputLabel.setBounds(10, 180, 80, 25);
        panel.add(inputLabel);

        inputArea = new JTextArea();
        inputArea.setBounds(100, 180, 210, 200);
        panel.add(inputArea);

        
        passOneButton = new JButton("Pass One");
        passOneButton.setBounds(330, 10, 120, 25);
        panel.add(passOneButton);

        passOneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                performPassOne();
            }
        });

        
        passTwoButton = new JButton("Pass Two");
        passTwoButton.setBounds(330, 50, 120, 25);
        panel.add(passTwoButton);

        passTwoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                performPassTwo();
            }
        });

        
        outputArea = new JTextArea();
        outputArea.setBounds(10, 390, 450, 400);
        panel.add(outputArea);
    }

    private void performPassOne() {
        String[] inputLines = inputArea.getText().split("\n");
        String[] optabLines = optabArea.getText().split("\n");
    
        
        for (String line : optabLines) {
            String[] parts = line.split("\\s+");
            if (parts.length == 2) {
                optab.put(parts[0], parts[1]);
            }
        }
    
        int locctr = 0;
        int start = 0;
    
        intermediate = new StringBuilder();
        symtabOutput = new StringBuilder();
    
        for (String line : inputLines) {
            if (line.trim().isEmpty()) continue; 
            String[] parts = line.split("\\s+");
    
            int opcodeIndex = (parts.length == 3) ? 1 : 0;
            int operandIndex = (parts.length == 3) ? 2 : 1;
    
            if (parts.length > opcodeIndex) {
                String label = (opcodeIndex == 1) ? parts[0] : ""; 
                String opcode = parts[opcodeIndex];
                String operand = (parts.length > operandIndex) ? parts[operandIndex] : "";
    
                if (opcode.equals("START")) {
                    try {
                        start = Integer.parseInt(operand, 16);
                        locctr = start;
                    } catch (NumberFormatException e) {
                        outputArea.setText("Invalid START address: " + operand);
                        return;
                    }
                    intermediate.append("\t").append(label).append("\t").append(opcode).append("\t").append(operand).append("\n");
                } else if (opcode.equals("END")) {
                    
                    intermediate.append(Integer.toHexString(locctr).toUpperCase()).append("\t").append(label).append("\t").append(opcode).append("\n");
                    break;
                } else {
                    
                    intermediate.append(Integer.toHexString(locctr).toUpperCase()).append("\t").append(label).append("\t").append(opcode).append("\t").append(operand).append("\n");
    
                    if (!label.isEmpty()) {
                        symtab.put(label, Integer.toHexString(locctr).toUpperCase());
                        symtabOutput.append(label).append("\t").append(Integer.toHexString(locctr).toUpperCase()).append("\n");
                    }
    
                    
                    if (optab.containsKey(opcode)) {
                        locctr += 3; 
                    } else if (opcode.equals("WORD")) {
                        locctr += 3; 
                    } else if (opcode.equals("RESW")) {
                        locctr += 3 * Integer.parseInt(operand); 
                    } else if (opcode.equals("BYTE")) {
                        if (operand.startsWith("C'")) {
                            locctr += operand.length() - 3; 
                        } else if (operand.startsWith("X'")) {
                            locctr += (operand.length() - 3) / 2; 
                        }
                    } else if (opcode.equals("RESB")) {
                        locctr += Integer.parseInt(operand); 
                    } else {
                        outputArea.setText("Invalid opcode: " + opcode);
                    }
                }
            } else {
                outputArea.setText("Invalid line format: " + line);
                return;
            }
        }
    
        finalLocctr = locctr;  
    
       
        StringBuilder output = new StringBuilder();
        output.append("SYMTAB:\n");
        output.append(symtabOutput.toString());
        output.append("\nIntermediate Code:\n");
        output.append(intermediate.toString());
    
        outputArea.setText(output.toString());
    }
    
    private void performPassTwo() {
        String[] intermediateLines = intermediate.toString().split("\n");
        StringBuilder objCodeOutput = new StringBuilder();
        StringBuilder objCodePerLineOutput = new StringBuilder();
        StringBuilder outputTable = new StringBuilder(); 
    
        String programName = "";
        String startAddress = "";
    
        boolean firstTextRecord = true;
        int currentTextStartAddress = -1;
        StringBuilder currentTextRecord = new StringBuilder();
        int currentTextLength = 0;
    
        
        outputTable.append(String.format("%-10s %-10s %-10s %-10s %-15s\n", "Address", "Label", "Opcode", "Operand", "Machine Code"));
        outputTable.append("--------------------------------------------------------------\n");
    
        
        for (String line : intermediateLines) {
            String[] parts = line.split("\\s+");
    
            if (parts.length >= 4) {
                String address = parts[0];
                String label = parts[1];
                String opcode = parts[2];
                String operand = parts.length > 3 ? parts[3] : "";
    
                
                if (opcode.equals("START")) {
                    programName = String.format("%-6s", label); 
                    startAddress = operand; 
                    currentTextStartAddress = Integer.parseInt(startAddress, 16);
                }
            }
        }
    
        
        int programLength = finalLocctr - currentTextStartAddress;
    
        
        objCodeOutput.append(String.format("H^%-6s^%06X^%06X\n", programName, currentTextStartAddress, programLength));
    
        for (String line : intermediateLines) {
            String[] parts = line.split("\\s+");
            String address = parts[0];
            String label = "", opcode = "", operand = "";
    
            if (parts.length == 4) {
                label = parts[1];
                opcode = parts[2];
                operand = parts[3];
            } else if (parts.length == 3) {
                opcode = parts[1];
                operand = parts[2];
            } else if (parts.length == 2) {
                opcode = parts[1];
            }
    
            String objCode = "";  
    
            
            if (optab.containsKey(opcode)) {
                objCode = optab.get(opcode);
                String operandAddress = "0000";
    
                if (!operand.isEmpty() && symtab.containsKey(operand)) {
                    operandAddress = String.format("%04X", Integer.parseInt(symtab.get(operand), 16));
                }
    
                objCode += operandAddress;
            } else if (opcode.equals("WORD")) {
                objCode = String.format("%06X", Integer.parseInt(operand));
            } else if (opcode.equals("BYTE")) {
                if (operand.startsWith("C'") && operand.endsWith("'")) {
                    String constant = operand.substring(2, operand.length() - 1);
                    for (int i = 0; i < constant.length(); i++) {
                        objCode += String.format("%02X", (int) constant.charAt(i));
                    }
                } else if (operand.startsWith("X'") && operand.endsWith("'")) {
                    objCode = operand.substring(2, operand.length() - 1);
                }
            }
    
           
            outputTable.append(String.format("%-10s %-10s %-10s %-10s %-15s\n", address, label, opcode, operand, objCode));
    
            
            if (!objCode.isEmpty()) {
                if (firstTextRecord) {
                    if (!address.isEmpty()) { 
                        currentTextStartAddress = Integer.parseInt(address, 16);
                        firstTextRecord = false;
                    }
                }
    
                if (currentTextLength + objCode.length() / 2 > 30) {  
                    objCodeOutput.append(String.format("T^%06X^%02X^%s\n", currentTextStartAddress, currentTextLength, currentTextRecord));
                    currentTextRecord = new StringBuilder();
                    if (!address.isEmpty()) {  
                        currentTextStartAddress = Integer.parseInt(address, 16);
                    }
                    currentTextLength = 0;
                }
    
                currentTextRecord.append("^").append(objCode);
                currentTextLength += objCode.length() / 2;
                objCodePerLineOutput.append(String.format("%s\t%s\n", address, objCode));
            }
        }
    
        if (currentTextLength > 0) {
            objCodeOutput.append(String.format("T^%06X^%02X^%s\n", currentTextStartAddress, currentTextLength, currentTextRecord));
        }
    
        
        objCodeOutput.append(String.format("E^%06X\n", currentTextStartAddress));
    
        
        outputArea.setText("");
        outputArea.append("Output Table:\n");
        outputArea.append(outputTable.toString());
        outputArea.append("\nObject Code:\n");
        outputArea.append(objCodeOutput.toString());
    }
}    
