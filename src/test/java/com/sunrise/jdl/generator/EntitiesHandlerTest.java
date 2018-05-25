package com.sunrise.jdl.generator;

import com.sunrise.jdl.generator.entities.Entity;
import com.sunrise.jdl.generator.entities.Field;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class EntitiesHandlerTest {

    /**
     * TODO я думаю нужен тест на небольшом колличенстве данных, 1-2 сущности, что бы проверить, что все поля устонавливаются правильно.
     */
    @Test
    public void testReadAll() {
        ArrayList<InputStream> streams = new ArrayList<>(2);
        streams.add(this.getClass().getResourceAsStream("/dictionary.csv"));
        streams.add(this.getClass().getResourceAsStream("/data.csv"));
        EntitiesHandler reader = new EntitiesHandler(streams);
        List<Entity> result = reader.readAll();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
    }

    @Test
    // На прмере чтения сущности ContactDataCorrection и DriverLicense
    public void validationOfFieldsTypeReading() {
        ArrayList<InputStream> streams = new ArrayList<>(1);
        streams.add(this.getClass().getResourceAsStream("/twoEntities.csv"));
        EntitiesHandler reader = new EntitiesHandler(streams);
        List<Entity> result = reader.readAll();
        Entity contacts = result.get(0);
        ArrayList<Field> contactsFields = contacts.getFields();

        Assert.assertNotNull(result);
        Assert.assertEquals("Address", contactsFields.get(0).getFieldType());
        Assert.assertEquals("Address", contactsFields.get(1).getFieldType());
        Assert.assertEquals("Address", contactsFields.get(2).getFieldType());
        Assert.assertEquals("PhoneNumber", contactsFields.get(3).getFieldType());
        Assert.assertEquals("EmailAddress", contactsFields.get(4).getFieldType());
        Assert.assertEquals("String", contactsFields.get(5).getFieldType());
        Assert.assertEquals("SocialNetwork", contactsFields.get(6).getFieldType());
        Assert.assertEquals("Список<Document>", contactsFields.get(7).getFieldType());

        Entity license = result.get(1);
        ArrayList<Field> licenseFields = license.getFields();

        Assert.assertEquals("Дата/время", licenseFields.get(0).getFieldType());
        Assert.assertEquals("String", licenseFields.get(1).getFieldType());
        Assert.assertEquals("String", licenseFields.get(2).getFieldType());
        Assert.assertEquals("String", licenseFields.get(3).getFieldType());
        Assert.assertEquals("String", licenseFields.get(4).getFieldType());
        Assert.assertEquals("Дата/время", licenseFields.get(5).getFieldType());
        Assert.assertEquals("Дата/время", licenseFields.get(6).getFieldType());
        Assert.assertEquals("String", licenseFields.get(7).getFieldType());
        Assert.assertEquals("Employee", licenseFields.get(8).getFieldType());
        Assert.assertEquals("Group", licenseFields.get(9).getFieldType());
        Assert.assertEquals("Список<Document>", licenseFields.get(10).getFieldType());
    }

    @Test
    public void testCorrectFieldsType() {
        ArrayList<InputStream> streams = new ArrayList<>(1);
        streams.add(this.getClass().getResourceAsStream("/twoEntities.csv"));
        EntitiesHandler entitiesHandler = new EntitiesHandler(streams);
        List<Entity> result = entitiesHandler.readAll();
        Entity contacts = result.get(0);
        ArrayList<Field> contactsFields = contacts.getFields();
        int numberOfCorreciton = entitiesHandler.correctsFieldsType(result);
        
        Assert.assertNotNull(result);
        Assert.assertEquals("Address", contactsFields.get(0).getFieldType());
        Assert.assertEquals("Address", contactsFields.get(1).getFieldType());
        Assert.assertEquals("Address", contactsFields.get(2).getFieldType());
        Assert.assertEquals("PhoneNumber", contactsFields.get(3).getFieldType());
        Assert.assertEquals("EmailAddress", contactsFields.get(4).getFieldType());
        Assert.assertEquals("String", contactsFields.get(5).getFieldType());
        Assert.assertEquals("SocialNetwork", contactsFields.get(6).getFieldType());
        Assert.assertEquals("Список<Document>", contactsFields.get(7).getFieldType());

        Entity license = result.get(1);
        ArrayList<Field> licenseFields = license.getFields();

        Assert.assertEquals("Instant", licenseFields.get(0).getFieldType());
        Assert.assertEquals("String", licenseFields.get(1).getFieldType());
        Assert.assertEquals("String", licenseFields.get(2).getFieldType());
        Assert.assertEquals("String", licenseFields.get(3).getFieldType());
        Assert.assertEquals("String", licenseFields.get(4).getFieldType());
        Assert.assertEquals("Instant", licenseFields.get(5).getFieldType());
        Assert.assertEquals("Instant", licenseFields.get(6).getFieldType());
        Assert.assertEquals("String", licenseFields.get(7).getFieldType());
        Assert.assertEquals("Employee", licenseFields.get(8).getFieldType());
        Assert.assertEquals("Group", licenseFields.get(9).getFieldType());
        Assert.assertEquals("Список<Document>", licenseFields.get(10).getFieldType());
        Assert.assertEquals(9, numberOfCorreciton);
    }
}