package com.sunrise.jdl.generator;

import com.sunrise.jdl.generator.entities.Entity;
import com.sunrise.jdl.generator.entities.Field;
import com.sunrise.jdl.generator.entities.Relation;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Считывает Entity, корректириреут их поля, создает их структуру,
 * записывает Entity и их структуру в файл
 */
public class EntitiesHandler {

    /**
     * Константые поля содержат номера ячеек в электронной таблице,
     * из которых беруться соотвествующие значения.
     */
    public static final int CLASSNAME = 1;
    public static final int FIELDNAME = 2;
    public static final int FIELDTYPE = 5;
    public static final int FIELDSIZE = 6;
    private List<InputStream> resources;


    public EntitiesHandler(List<InputStream> resources) {
        this.resources = resources;
    }

    public List<Entity> readAll() {
        ArrayList<Entity> entities = new ArrayList<Entity>();
        for (InputStream st : resources) {
            entities.addAll(readDataFromCSV(st));
        }
        return entities;
    }

    private java.util.Collection<Entity> readDataFromCSV(InputStream stream) {
        Map<String, Entity> toReturn = new LinkedHashMap<String, Entity>();
        try {
            Reader in = new InputStreamReader(stream);
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
            String className = "";
            for (CSVRecord record : records) {
                String s1 = record.get(CLASSNAME);
                String fieldName = record.get(FIELDNAME);
                String fieldType = record.get(FIELDTYPE);
                String fieldLength = record.get(FIELDSIZE);

                if (!s1.equals("") && !s1.contains("П")) {
                    className = s1;
                    Field field = new Field(fieldType, fieldName, fieldLength);
                    ArrayList<Field> arrayList = new ArrayList<Field>();
                    arrayList.add(field);
                    Entity entity = new Entity(className, arrayList);
                    toReturn.put(className, entity);
                } else if (s1.equals("") && toReturn.size() > 0) {
                    toReturn.get(className).getFields().add(new Field(fieldType, fieldName, fieldLength));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Can't find file");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO exception");
            e.printStackTrace();
        }
        return toReturn.values();
    }

    /**
     * Перегруженный вариант correctsFieldsType(Entity entity).
     * Принимает List<Entity> и корректирует у всех entity поля.
     * @param entities
     * @return summer number of correction for all entities
     */
    public int correctsFieldsType(List<Entity> entities) {
        int sumOfCorrection = 0;
        for (Entity entity : entities) {
            sumOfCorrection += this.correctsFieldsType(entity);
        }
        return sumOfCorrection;
    }

    /**
     * Метод корректирует тип полей у отдельной сущности в соотвествии с требованиями jdl.
     * @param entity
     * @return number of corrections
     */
    public int correctsFieldsType(Entity entity) {
        int numberOfCorrection = 0;
        for (Field field : entity.getFields()) {
            String fieldType = field.getFieldType();
            if (fieldType.contains("Строка")) {
                fieldType = "String";
                numberOfCorrection++;
            }
            if (fieldType.contains("Дата")) {
                fieldType = "Instant";
                numberOfCorrection++;
            }
            if (fieldType.contains("Число")) {
                fieldType = "Long";
                numberOfCorrection++;
            }
        }
        return numberOfCorrection;
    }

    /**
     * Если сущность содержит в fields Список, метод создает объект Relation и
     * добавляет его в поле relations у Entity.
     * Метод возращает количество считанных структур.
     * @param entity
     * @return number of created structure
     */
    public int createStructure(Entity entity) {
        int count = 0;
        ArrayList<Field> fields = entity.getFields();
        StringBuilder builder = new StringBuilder();
        for (Field field : fields) {
            if (field.isEntity()) {
                count++;
                String fieldType = field.getFieldType();
                int start = fieldType.indexOf("<");
                int finish = fieldType.indexOf(">");
                String entityType = fieldType.substring(start + 1, finish);
                entity.getRelations().add(new Relation(entity.getClassName(), entityType, Relation.RelationType.OneToMany));
            }
        }
        return count;
    }

}
