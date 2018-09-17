package com.sunrise.jdl.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunrise.jdl.generator.actions.Action;
import com.sunrise.jdl.generator.entities.Entity;
import com.sunrise.jdl.generator.entities.Field;
import com.sunrise.jdl.generator.entities.ResultWithWarnings;
import com.sunrise.jdl.generator.forJson.BaseData;
import com.sunrise.jdl.generator.forJson.BaseField;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class EntityTypeService {

    public static final int TYPE_NAME = 1;
    public static final int SUBTYPE_NAME = 3;


    /**
     * Гененирует карту со связями НазваниеРодителя - Список<Названия потомков> из CSV
     *
     * @param resource CSV-файл, в котором описаны отношения между сущностями
     * @return Карта с описанием связей, но самих сущностей нет
     */
    public Map<String, List<String>> readCsv(InputStream resource) {
        Map<String, List<String>> result = new HashMap<>();
        try {
            Reader in = new InputStreamReader(resource);
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
            String lastType = "";
            for (CSVRecord record : records) {
                String fieldType = record.get(TYPE_NAME).trim();
                String fieldSubtype = record.get(SUBTYPE_NAME).trim();
                if (!fieldSubtype.isEmpty()) {
                    if (fieldType.equals("")) {
                        if (lastType.equals(""))
                            throw new IOException();
                        result.get(lastType).add(fieldSubtype);
                    } else {
                        lastType = fieldType;
                        result.put(fieldType, new ArrayList<>());
                        result.get(fieldType).add(fieldSubtype);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("Can't find file");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO exception");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Перегруженный вариант метода prepareDataForParentEntity(String parentName, List<Entity> childrenEntities).
     * В цикле проходится по Map<String, List<Entity>> parentNameAndChildrenEntities и для каждого Map.Entry<String, List<Entity>>
     * вызывается метод prepareDataForParentEntity.
     *
     * @param parentNameAndChildrenEntities - Map<String, List<Entity>> карта в качестве ключа содержит имя родителя, в качестве значений -
     *                                      список дочерних полей
     * @return
     */
    public Map<String, Set<Field>> prepareDataForParentEntity(Map<String, List<Entity>> parentNameAndChildrenEntities) {
        Map<String, Set<Field>> allParents = new HashMap<>();
        for (Map.Entry<String, List<Entity>> relation : parentNameAndChildrenEntities.entrySet()) {
            Map<String, Set<Field>> singleParent = prepareDataForParentEntity(relation.getKey(), relation.getValue());
            allParents.putAll(singleParent);
        }
        return allParents;
    }

    /**
     * Метод подсчитывает частоту полей у дочерних сущностей List<Entity> childrenEntities и оставляет поля, которые
     * есть у всех сущностей. Возвращает имя родителя и список общих полей
     *
     * @param parentName       - имя родительской сущности
     * @param childrenEntities - список дочерних сущностей
     * @return Map<String                                                                                                                                                                                                                                                               ,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               Set                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                               Field>> result - имя родителя и список его полей
     */
    public Map<String, Set<Field>> prepareDataForParentEntity(String parentName, List<Entity> childrenEntities) {
        Map<Field, Byte> fieldsWithFrequency = new HashMap<>();
        for (Entity entity : childrenEntities) {
            for (Field field : entity.getFields()) {
                if (fieldsWithFrequency.containsKey(field)) {
                    fieldsWithFrequency.put(field, (byte) (fieldsWithFrequency.get(field) + 1));
                } else {
                    fieldsWithFrequency.put(field, (byte) 1);
                }
            }
        }
        fieldsWithFrequency.entrySet().removeIf(pair -> pair.getValue() < childrenEntities.size());
        Map<String, Set<Field>> result = new HashMap<>();
        result.put(parentName, fieldsWithFrequency.keySet());
        return result;
    }

    /**
     * Сгруппировать сущности относительно родителькой группы.
     *
     * @param entities Список доступных сущностей
     * @param typesMap Словарь где ключ - название группы сущности, а значение название сущностей которые входят в эту группу
     * @return Список сущностей сгруппированный по группам из @typesMap, а так же список возниших при группировке предупреждений
     */
    public ResultWithWarnings<Map<String, List<Entity>>> mergeTypesWithThemSubtypes(Collection<Entity> entities, Map<String, List<String>> typesMap) {
        List<String> warnings = new ArrayList<>();
        Map<String, List<Entity>> result = typesMap.keySet().stream().collect(Collectors.toMap(x -> x, x -> new LinkedList<>()));
        Map<String, Entity> entityMap = entities.stream().collect(Collectors.toMap(x -> x.getClassName(), x -> x));
        for (String group : typesMap.keySet()) {
            for (String entityName : typesMap.get(group)) {
                Entity entity = entityMap.get(entityName);
                if (entity == null) {
                    warnings.add(String.format("В меню объявлена сущность под названем [%s], однако в списке загруженных сущностей она отсуствует", entityName));
                } else {
                    result.get(group).add(entity);
                }
            }
        }
        return new ResultWithWarnings<>(warnings, result);
    }


    public void writeToJsonFile(String fileWithActions, String destinationFolder, Map<String, Set<Field>> parentWithFields) throws IOException, URISyntaxException {
        ActionService actionService = new ActionService();
        InputStream stream = this.getClass().getResourceAsStream(fileWithActions);
        Collection<Action> actions = actionService.readDataFromCSV(stream);

        List<BaseData> baseDataList = convertToBaseDataAndBaseField(parentWithFields, (List) actions);
        ObjectMapper mapper = new ObjectMapper();

        URL resource = this.getClass().getResource(destinationFolder);
        System.out.println(resource.toURI());
        System.out.println(resource.toString());
        Path targetFolder = Paths.get(String.valueOf(new File(resource.toURI())));

        Files.walkFileTree(targetFolder, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        for (BaseData baseData : baseDataList) {
            Path baseDataPath = Files.createDirectories(Paths.get(destinationFolder + "/" + baseData.getName() ));
            mapper.writeValue(new File(baseDataPath + "/" + baseData.getName() + ".json"), baseData);
        }

    }




    /**
     * Конвертация в BaseData
     * @param parentWithFields
     * @return
     */
    private List<BaseData> convertToBaseDataAndBaseField(Map<String, Set<Field>> parentWithFields, List actions) {
        List<BaseData> listBaseData = new ArrayList<>();
        for (Map.Entry<String, Set<Field>> entry : parentWithFields.entrySet()) {
            BaseData baseData = new BaseData(entry.getKey());
            for (Field field : entry.getValue()) {
                baseData.getListFields().add(new BaseField(field));
                baseData.setActions(actions);
            }
            listBaseData.add(baseData);
        }
        return listBaseData;
    }
}
