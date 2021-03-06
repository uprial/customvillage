package com.gmail.uprial.customvillage.storage;

import com.gmail.uprial.customvillage.common.CustomLogger;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Map.Entry;

public class CustomStorage {
    private static final Character VALUE_DELIMITER = '=';

    private final File dataFolder;
    private final String fileName;
    private final CustomLogger customLogger;

    private StorageData lastData;

    public CustomStorage(File dataFolder, String fileName, CustomLogger customLogger) {
        this.dataFolder = dataFolder;
        this.fileName = fileName;
        this.customLogger = customLogger;
    }

    public void save(StorageData data) {
        if(data.equals(lastData)) {
            customLogger.debug(String.format("Skipping write to %s: data has not been changed", getFileName()));
            return;
        }

        try {
            if (!dataFolder.exists()) {
                if (!dataFolder.mkdir()) {
                    customLogger.error(String.format("Can't create directory %s", dataFolder.getPath()));
                }
            }

            try {
                saveData(data);
            } catch (IOException e) {
                customLogger.error(e.toString());
            }
        } finally {
            lastData = data;
        }
    }

    public StorageData load() {
        StorageData data = new StorageData();
        try {
            final File file = new File(getFileName());
            if (file.exists()) {
                try {
                    data = loadData();
                } catch (IOException e) {
                    customLogger.error(e.toString());
                }
            }

            return data;
        } finally {
            lastData = data;
        }
    }

    private void saveData(StorageData data) throws IOException {
        final String filename = getFileName();

        if(!data.isEmpty()) {
            try(FileWriter fileWriter = new FileWriter(filename)) {
                try(BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

                    String[] row = new String[2];
                    for (Entry<String, String> entry : data.entrySet()) {
                        row[0] = entry.getKey();
                        row[1] = entry.getValue();

                        bufferedWriter.write(StringUtils.join(row, VALUE_DELIMITER));
                        bufferedWriter.newLine();
                    }
                }
            }
        } else {
            final File file = new File(filename);
            if(file.exists()) {
                if (!file.delete()) {
                    customLogger.error(String.format("Can't delete file %s", filename));
                }
            }
        }
    }

    private StorageData loadData() throws IOException {
        final StorageData data = new StorageData();

        try(FileReader fileReader = new FileReader(getFileName())) {
            try(BufferedReader bufferedReader = new BufferedReader(fileReader)) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] row = StringUtils.split(line, VALUE_DELIMITER);
                    data.put(row[0], row[1]);
                }
            }
        }

        return data;
    }

    private String getFileName() {
        final File file = new File(dataFolder, fileName);
        return file.getPath();
    }

}
