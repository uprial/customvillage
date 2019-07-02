package com.gmail.uprial.customvillage.storage;

import com.gmail.uprial.customvillage.common.CustomLogger;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class CustomStorage {
    private static final Character VALUE_DELIMITER = '=';

    private final File dataFolder;
    private final String fileName;
    private final CustomLogger customLogger;

    public CustomStorage(File dataFolder, String fileName, CustomLogger customLogger) {
        this.dataFolder = dataFolder;
        this.fileName = fileName;
        this.customLogger = customLogger;
    }

    public void save(Map<String,String> data) {
        if(!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdir();
        }

        try {
            saveData(data);
        } catch (IOException e) {
            customLogger.error(e.toString());
        }
    }

    public Map<String,String> load() {
        File file = new File(getFileName());
        if(file.exists()) {
            try {
                return loadData();
            } catch (IOException e) {
                customLogger.error(e.toString());
            }
        }

        return new HashMap<>();
    }

    private void saveData(Map<String,String> data) throws IOException {
        FileWriter fileWriter = new FileWriter(getFileName());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        String[] row = new String[2];
        for (Entry<String,String> entry : data.entrySet()) {
            row[0] = entry.getKey();
            row[1] = entry.getValue();

            bufferedWriter.write(StringUtils.join(row, VALUE_DELIMITER));
            bufferedWriter.newLine();
        }

        bufferedWriter.close();
    }

    private Map<String,String> loadData() throws IOException {
        final Map<String,String> data = new HashMap<>();

        FileReader fileReader = new FileReader(getFileName());
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while((line = bufferedReader.readLine()) != null) {
            String[] row = StringUtils.split(line, VALUE_DELIMITER);
            data.put(row[0], row[1]);
        }

        bufferedReader.close();

        return data;
    }

    private String getFileName() {
        File file = new File(dataFolder, fileName);
        return file.getPath();
    }

}
