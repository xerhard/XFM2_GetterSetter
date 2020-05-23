/*
 * XFM2_GetterSetter
 *
 * This is a javaFX tool to replace the XFM2 spreadsheet and use XFM2 json patch files
 *
 *
 * This file is part of the XFM2_GetterSetter distribution.
 * - (https://github.com/xerhard/XFM2_GetterSetter).
 * Copyright (c) 2020 Gerhard Peper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


package eu.GPio;

import com.fazecast.jSerialComm.*;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import static java.nio.file.Files.readString;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class SetParFX4  extends Application {

    TextArea JArea = new TextArea();
    String JAreaTxt = "";
    TextArea SysLog = new TextArea();
    String SysLogTxt = "";
    TextArea PArea = new TextArea();
    String PAreaTxt = "0";
    String patchname = "patchname";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("XFM2 GetterSetter");

        FileChooser fileChooser = new FileChooser();

        Button Rbutton = new Button("Open XFM2 patch File");
        Button Sbutton = new Button("Save XFM2 patch File");
        Button Gbutton = new Button("Get Program from buffer");
        Button Pbutton = new Button("Put Program into buffer");
        Button Ibutton = new Button("Init: reads default init file");
        Button rPbutton = new Button("Read Program Preset 1 ... 128 into buffer");
        Button wPbutton = new Button("Write buffer to Program Preset 1 ... 128");

        Rbutton.setOnAction(e -> {
            fileChooser.setTitle("Open XFM2 patch file");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            System.out.println(selectedFile.toString());
            readFile(selectedFile.toString());
        });

        Sbutton.setOnAction(e -> {
            fileChooser.setTitle("Save XFM2 patch file");
            getPatchname();
            fileChooser.setInitialFileName(patchname+".json");
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            System.out.println(selectedFile.toString());
            writeFile(selectedFile.toString());
        });

        Gbutton.setOnAction(e -> {
            getProg();
        });

        Pbutton.setOnAction(e -> {
            putProg();
        });

        Ibutton.setOnAction(e -> {
            readFile("./000init.json");
        });

        rPbutton.setOnAction(e -> {
            readPreset();
        });

        wPbutton.setOnAction(e -> {
            writePreset();
        });

        VBox vBox = new VBox(Rbutton, Sbutton, Gbutton, Pbutton, rPbutton, wPbutton, Ibutton, PArea, JArea, SysLog);
        JArea.setPrefRowCount(30);
        PArea.setPrefRowCount(0);
        SysLog.setPrefRowCount(5);
        Scene scene = new Scene(vBox, 400, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        System.out.println("Working Directory = " + System.getProperty("user.dir")); 
    }


    //  read json data from file to JArea
    public void readFile(String fJson) {
        try {
            JAreaTxt = readString(Paths.get(fJson), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        JArea.setText(JAreaTxt);
    }


    // get short patchname
    public void getPatchname() {
        String JsonStr = JArea.getText();
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(JsonStr);
            patchname = (String) jsonObject.get("Short_Name");
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }


    //  write json data from Jarea to file
    public void writeFile(String fJson) {
        String JsonStr = JArea.getText();
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(JsonStr);
            //System.out.println(jsonObject.toJSONString());
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            String strDate = dateFormat.format(date);
            jsonObject.put("date", strDate);

            // Build Parameter string for json file to write to
            String parStr = "\t\"parameters\": [\n";
            int count = 0;
            long[] numarr = new long[512];
            long lPar, lVal;
            JSONArray array = (JSONArray) jsonObject.get("parameters");
            Iterator<JSONObject> iterator = array.iterator();
            while (iterator.hasNext()) {
                JSONObject element = (JSONObject) iterator.next();
                lPar = (long) element.get("Par#");
                lVal = (long) element.get("Value");
                numarr[count] = lVal;
                if (count != 0) {
                    parStr = parStr + " ,\n";
                }
                parStr = parStr + "\t\t{\"Par#\": " + lPar + " , \"Value\": " + lVal + "}";
                count++;
            }
            parStr = parStr + "\n\t]\n}\n";

            // Calculate hex hash value en write to jsonObject
            byte arr[] = new byte[count];
            for (int x = 0; x < count; x++) {
                arr[x] = (byte) (numarr[x] & 0xff);
            }
            long hash = Arrays.hashCode(arr);
            String hashStr = Integer.toHexString((int) hash);
            jsonObject.put("hash", hashStr);

            // Build header string for json file to write to
            String headStr = "{\n";
            String KeyStr = (String) jsonObject.get("Short_Name");
            headStr = headStr + "\t\"Short_Name\": \"" + KeyStr + "\",\n";
            KeyStr = (String) jsonObject.get("Long_Name");
            headStr = headStr + "\t\"Long_Name\": \"" + KeyStr + "\",\n";
            KeyStr = (String) jsonObject.get("Description");
            headStr = headStr + "\t\"Description\": \"" + KeyStr + "\",\n";
            KeyStr = (String) jsonObject.get("Creator");
            headStr = headStr + "\t\"Creator\": \"" + KeyStr + "\",\n";
            KeyStr = (String) jsonObject.get("date");
            headStr = headStr + "\t\"date\": \"" + KeyStr + "\",\n";
            KeyStr = (String) jsonObject.get("hash");
            headStr = headStr + "\t\"hash\": \"" + KeyStr + "\",\n";

            JArea.setText(headStr + parStr);

            try (FileWriter writer = new FileWriter(fJson)) {
                writer.write(headStr + parStr);
                writer.flush();
                SysLog.setText("Saved Program to: " + fJson + "\n");
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    // get json parameter values from XFM2
    public void getProg() {

        int i;
        int instr = 0;
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        String strDate = dateFormat.format(date);

        SerialPort[] ports = SerialPort.getCommPorts();
        String[] result = new String[ports.length];

        for (i = 0; i < ports.length; i++) {
            result[i] = ports[i].getSystemPortName();
            System.out.println(i + " " + result[i]);
        }
        System.out.println("Aantal gevonden poorten: " + ports.length);


        for (i = 0; i < ports.length; i++) {
            SerialPort comPort = ports[i];
            // System.out.println("Going to use the following port: " + comPort.getSystemPortName());

            comPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            comPort.setComPortParameters(500000, 8,
                    SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            comPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                    1000, 1000);

            boolean Presult = comPort.openPort();

            if ( ports[i].getSystemPortName().matches("ttyUSB.")  ||
                 ports[i].getSystemPortName().matches("cu.usbserial-.........FCC.") ||
                 ports[i].getSystemPortName().equals("COM4") ) {

                System.out.println("Found serial device");

                try (OutputStream out = ports[i].getOutputStream()) {
                    //==========================
                    // 105             i Init               Return: 0
                    // 100             d Display            Return: 512 Bytes
                    // 115, 33, 48     s Set par 33 to 48   Return: -
                    // 103, 33         g Get par 33         Return: parameter value unsigned byte
                    // 36              $ Clear EEPROM !!    Return: -
                    //==========================
                    int numarr[] = {100};  // Get all parameters from actual Program and save in JSON file
                    instr = numarr[0];
                    byte arr[] = new byte[numarr.length];
                    for (int x = 0; x < numarr.length; x++) {
                        arr[x] = (byte) (numarr[x] & 0xff);
                    }
                    //==========================
                    out.write(arr);
                    System.out.println("Send message to:  ttyUSB1 , Length sent message: " + arr.length);
                    SysLogTxt = SysLogTxt + "Send message to:  ttyUSB1\nLength sent message: " + arr.length;
                    SysLog.setText(SysLogTxt);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (instr != 115 && instr != 36) {
                    try (InputStream in = ports[i].getInputStream()) {
                        // Read from serial device
                        byte[] buffer = new byte[512];
                        boolean end = false;
                        String message = "";


                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        int len = in.read(buffer);
                        System.out.println("Number of bytes read: " + len);
                        SysLogTxt = SysLogTxt + "Number of bytes read: " + len;
                        SysLog.setText(SysLogTxt);

                        comPort.closePort();

                        int hash = Arrays.hashCode(buffer);

                        // Build jason String

                        JAreaTxt = "{\n\t\"Short_Name\": \"noname\",\n";
                        JAreaTxt = JAreaTxt + "\t\"Long_Name\": \"noname\",\n";
                        JAreaTxt = JAreaTxt + "\t\"Description\": \"nodescription\",\n";
                        JAreaTxt = JAreaTxt + "\t\"Creator\": \"Design.Thoughtz.eu\",\n";
                        JAreaTxt = JAreaTxt + "\t\"date\": \"" + strDate + "\",\n";
                        JAreaTxt = JAreaTxt + "\t\"hash\": \"" + Integer.toHexString((int) hash) + "\",\n";
                        JAreaTxt = JAreaTxt + "\t\"parameters\": [\n";

                        for (int j = 0; j < len; j++) {
                            //System.out.println("Par#: "+ j + " , Value: " + Byte.toUnsignedInt(buffer[j]));
                            if (j != len - 1) {
                                JAreaTxt = JAreaTxt + "\t\t{\"Par#\": " + j + " , \"Value\": " + Byte.toUnsignedInt(buffer[j]) + "} ,\n";
                            } else {
                                JAreaTxt = JAreaTxt + "\t\t{\"Par#\": " + j + " , \"Value\": " + Byte.toUnsignedInt(buffer[j]) + "}\n";
                            }
                        }
                        JAreaTxt = JAreaTxt + "\t]\n}\n";
                        JArea.setText(JAreaTxt);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    // send json parameter values to XFM2
    public void putProg() {
        int i;
        int instr = 0;

        SysLogTxt = "";
        SysLog.setText(SysLogTxt);


        SerialPort[] ports = SerialPort.getCommPorts();
        String[] result = new String[ports.length];
        for (i = 0; i < ports.length; i++) {
            result[i] = ports[i].getSystemPortName();
            System.out.println(i + " " + result[i]);
        }
        System.out.println("Aantal gevonden poorten: " + ports.length);

        if (ports.length == 0) {
            SysLogTxt = SysLogTxt + "No serial device found !!\nCheck XFM2 to be connected\n";
            SysLog.setText(SysLogTxt);
        }

        for (i = 0; i < ports.length; i++) {
            SerialPort comPort = ports[i];

            comPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            comPort.setComPortParameters(500000, 8,
                    SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            comPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                    1000, 1000
            );
            comPort.openPort();

            if (ports[i].getSystemPortName().equals("ttyUSB1")) {

                System.out.println("Found ttyUSB1");
                SysLogTxt = SysLogTxt + "Found ttyUSB1\nStart sending Parameters from: ";
                SysLog.setText(SysLogTxt);


                try (OutputStream Sout = ports[i].getOutputStream()) {
                    // JSON Begin
                    String JsonStr = JArea.getText();
                    System.out.println(JsonStr);
                    JSONParser parser = new JSONParser();

                    try {
                        JSONObject jsonObject = (JSONObject) parser.parse(JsonStr);
                        System.out.println(">>>>" + jsonObject.toString() + "\n<<<<\n");
                        // Show json header info
                        String KeyStr = (String) jsonObject.get("Short_Name");
                        System.out.println(KeyStr);
                        SysLogTxt = SysLogTxt + KeyStr + "\n";
                        SysLog.setText(SysLogTxt);

                        String date = (String) jsonObject.get("date");
                        System.out.println(date);
                        int ParNo = 0;
                        int Value = 0;
                        int counter = 0;
                        // loop array
                        JSONArray array = (JSONArray) jsonObject.get("parameters");
                        System.out.println(array.toString());
                        Iterator<JSONObject> iterator = array.iterator();
                        while (iterator.hasNext()) {
                            JSONObject element = (JSONObject) iterator.next();
                            long par = (long) element.get("Par#");
                            long val = (long) element.get("Value");
                            System.out.println("Par# = " + par + "  Value = " + val);

                            if (par > 254) {
                                long numarr[] = {115, 255, par - 256, val};
                                byte arr[] = new byte[numarr.length];
                                for (int x = 0; x < numarr.length; x++) {
                                    arr[x] = (byte) (numarr[x] & 0xff);
                                }
                                Sout.write(arr);
                                //System.out.println(Arrays.toString(arr));
                            } else {
                                long numarr[] = {115, par, val};
                                byte arr[] = new byte[numarr.length];
                                for (int x = 0; x < numarr.length; x++) {
                                    arr[x] = (byte) (numarr[x] & 0xff);
                                }
                                Sout.write(arr);
                                //System.out.println(Arrays.toString(arr));
                            }
                            counter++;
                        }
                        SysLogTxt = SysLogTxt + "Sent " + counter + " parameter values\n";
                        SysLog.setText(SysLogTxt);

                        // JSON End
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                comPort.closePort();
            }
        }
    }

    // read given preset from XFM2 into XFM2 buffer
    public void readPreset() {

        int i = Integer.parseInt(PArea.getText());
        if (i >= 1 & i <= 128) {
            SysLog.setText("Value is valid : " + i + "\n");
            long rarr[] = { 114, i-1 };
            shoot(rarr);
        } else {
            SysLog.setText("Value is not within range 0 .. 255\n" );
        }



    }

    // write XFM2 buffer content to preset given position
    public void writePreset() {
        int i = Integer.parseInt(PArea.getText());
        if (i >= 1 & i <= 128) {
            SysLog.setText("Value is valid : " + i );
            long warr[] = { 119, i-1 };
            shoot(warr);
        } else {
            SysLog.setText("Value is not within range 0 .. 255\n" );
        }

    }

    public void shoot(long numarr[]) {

       // SysLogTxt = "Writing buffer to ";
       // SysLog.setText(SysLogTxt);
        long instr = numarr[0];

        SerialPort[] ports = SerialPort.getCommPorts();
        String[] result = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            result[i] = ports[i].getSystemPortName();
            System.out.println(i + " " + result[i]);
        }
        System.out.println("Aantal gevonden poorten: " + ports.length);

        if (ports.length == 0) {
            SysLogTxt = SysLogTxt + "No serial device found !!\nCheck XFM2 to be connected\n";
            SysLog.setText(SysLogTxt);
            return;
        }

        for (int i = 0; i < ports.length; i++) {
            SerialPort comPort = ports[i];

            comPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            comPort.setComPortParameters(500000, 8,
                    SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            comPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                    1000, 1000
            );
            comPort.openPort();

            if (ports[i].getSystemPortName().equals("ttyUSB1")) {

                System.out.println("Found ttyUSB1");
                SysLogTxt = SysLogTxt + "Found ttyUSB1\nStart sending Parameters from: ";
                SysLog.setText(SysLogTxt);
                
                try (OutputStream out = ports[i].getOutputStream()) {
                    //==========================
                    // 105             i Init               Return: 0
                    // 100             d Display            Return: 512 Bytes
                    // 115, 33, 48     s Set par 33 to 48   Return: -
                    // 103, 33         g Get par 33         Return: parameter value unsigned byte
                    // 36              $ Clear EEPROM !!    Return: -
                    //==========================

                    byte arr[] = new byte[numarr.length];
                    for (int x = 0; x < numarr.length; x++) {
                        arr[x] = (byte) (numarr[x] & 0xff);
                    }
                    //==========================
                    out.write(arr);
                    System.out.println("Send message to:  ttyUSB1 , Length sent message: " + arr.length);
                    SysLogTxt = SysLogTxt + "Send message to:  ttyUSB1\nLength sent message: " + arr.length + "\n";
                    SysLog.setText(SysLogTxt);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (instr != 115 && instr != 36) {
                    try (InputStream in = ports[i].getInputStream()) {
                        // Read from serial device
                        byte[] buffer = new byte[1];
                        boolean end = false;
                        String message = "";


                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        int len = in.read(buffer);
                        System.out.println("Number of bytes read: " + len);
                        SysLogTxt = SysLogTxt + "Number of bytes read: " + len + "\n";
                        SysLog.setText(SysLogTxt);

                        comPort.closePort();

                        if (buffer[0]==0) {
                            SysLogTxt = SysLogTxt + "Handling preset succesfully\n";
                        } else {
                            SysLogTxt = SysLogTxt + "Handling preset failed, try again ....\n";
                        }
                        SysLog.setText(SysLogTxt);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
