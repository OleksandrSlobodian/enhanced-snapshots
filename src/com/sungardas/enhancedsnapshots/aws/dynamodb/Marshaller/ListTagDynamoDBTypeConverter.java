package com.sungardas.enhancedsnapshots.aws.dynamodb.Marshaller;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.amazonaws.services.ec2.model.Tag;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.List;

public class ListTagDynamoDBTypeConverter implements DynamoDBTypeConverter<String, List<Tag>> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectWriter writer = mapper.writer();

    @Override
    public String convert(List object) {
        try {
            return writer.writeValueAsString(object);
        } catch (final Exception e) {
            throw new DynamoDBMappingException("Unable to write object to JSON", e);
        }
    }

    @Override
    public List<Tag> unconvert(String object) {
        try {
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Tag.class);
            return mapper.readValue(object, type);
        } catch (final Exception e) {
            throw new DynamoDBMappingException("Unable to read JSON string", e);
        }
    }
}
