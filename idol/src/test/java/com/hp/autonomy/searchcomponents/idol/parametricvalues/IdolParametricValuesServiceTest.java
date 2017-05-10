/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.searchcomponents.idol.parametricvalues;

import com.autonomy.aci.client.util.AciParameters;
import com.google.common.collect.ImmutableMap;
import com.hp.autonomy.searchcomponents.core.fields.TagNameFactory;
import com.hp.autonomy.searchcomponents.core.parametricvalues.BucketingParams;
import com.hp.autonomy.searchcomponents.core.parametricvalues.BucketingParamsHelper;
import com.hp.autonomy.searchcomponents.core.parametricvalues.DependentParametricField;
import com.hp.autonomy.searchcomponents.core.test.CoreTestContext;
import com.hp.autonomy.searchcomponents.idol.fields.IdolFieldsRequestBuilder;
import com.hp.autonomy.searchcomponents.idol.fields.IdolFieldsService;
import com.hp.autonomy.searchcomponents.idol.search.HavenSearchAciParameterHandler;
import com.hp.autonomy.searchcomponents.idol.search.QueryExecutor;
import com.hp.autonomy.types.idol.responses.DateOrNumber;
import com.hp.autonomy.types.idol.responses.FlatField;
import com.hp.autonomy.types.idol.responses.GetQueryTagValuesResponseData;
import com.hp.autonomy.types.idol.responses.RecursiveField;
import com.hp.autonomy.types.idol.responses.TagValue;
import com.hp.autonomy.types.idol.responses.Values;
import com.hp.autonomy.types.requests.idol.actions.tags.DateValueDetails;
import com.hp.autonomy.types.requests.idol.actions.tags.FieldPath;
import com.hp.autonomy.types.requests.idol.actions.tags.NumericValueDetails;
import com.hp.autonomy.types.requests.idol.actions.tags.QueryTagInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.RangeInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.TagName;
import com.hp.autonomy.types.requests.idol.actions.tags.ValueDetails;
import com.hp.autonomy.types.requests.idol.actions.tags.params.FieldTypeParam;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.hp.autonomy.searchcomponents.core.test.CoreTestContext.CORE_CLASSES_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = CoreTestContext.class, properties = CORE_CLASSES_PROPERTY)
public class IdolParametricValuesServiceTest {
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Mock
    private HavenSearchAciParameterHandler parameterHandler;

    @Mock
    private IdolFieldsService fieldsService;

    @Mock
    private ObjectFactory<IdolFieldsRequestBuilder> fieldsRequestBuilderFactory;

    @Mock
    private IdolFieldsRequestBuilder fieldsRequestBuilder;

    @Autowired
    private BucketingParamsHelper bucketingParamsHelper;

    @Autowired
    private TagNameFactory tagNameFactory;

    @Mock
    private JAXBElement<Serializable> element;

    @Mock
    private QueryExecutor queryExecutor;

    private IdolParametricValuesService parametricValuesService;

    @SuppressWarnings("CastToConcreteClass")
    @Before
    public void setUp() {
        when(fieldsRequestBuilderFactory.getObject()).thenReturn(fieldsRequestBuilder);
        when(fieldsRequestBuilder.fieldType(any())).thenReturn(fieldsRequestBuilder);

        parametricValuesService = new IdolParametricValuesServiceImpl(
                parameterHandler,
                fieldsService,
                fieldsRequestBuilderFactory,
                bucketingParamsHelper,
                tagNameFactory,
                queryExecutor
        );
    }

    @Test
    public void getParametricValues() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.singletonList("Some field"));

        final GetQueryTagValuesResponseData responseData = mockQueryResponse();
        when(queryExecutor.executeGetQueryTagValues(any(AciParameters.class), any())).thenReturn(responseData);
        final Set<QueryTagInfo> results = parametricValuesService.getParametricValues(idolParametricRequest);
        assertThat(results, is(not(empty())));
    }

    @Test
    public void getFieldNamesFirst() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.emptyList());

        final ImmutableMap<FieldTypeParam, Set<TagName>> fieldsResponse = ImmutableMap.of(FieldTypeParam.Parametric, Collections.singleton(tagNameFactory.buildTagName("CATEGORY")));
        when(fieldsService.getFields(any())).thenReturn(fieldsResponse);

        final GetQueryTagValuesResponseData responseData = mockQueryResponse();
        when(queryExecutor.executeGetQueryTagValues(any(AciParameters.class), any())).thenReturn(responseData);

        final Set<QueryTagInfo> results = parametricValuesService.getParametricValues(idolParametricRequest);
        assertThat(results, is(not(empty())));
    }

    @Test
    public void parametricValuesNotConfigured() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.emptyList());

        final Map<FieldTypeParam, Set<TagName>> response = ImmutableMap.of(FieldTypeParam.Parametric, Collections.emptySet());
        when(fieldsService.getFields(any())).thenReturn(response);

        final Set<QueryTagInfo> results = parametricValuesService.getParametricValues(idolParametricRequest);
        assertThat(results, is(empty()));
    }

    @Test
    public void getNumericValueDetailsNoFields() {
        final IdolParametricRequest parametricRequest = mockRequest(Collections.emptyList());
        assertThat(parametricValuesService.getNumericValueDetails(parametricRequest).size(), is(0));
    }

    @Test
    public void getDateValueDetailsNoFields() {
        final IdolParametricRequest parametricRequest = mockRequest(Collections.emptyList());
        assertThat(parametricValuesService.getDateValueDetails(parametricRequest).size(), is(0));
    }

    @Test
    public void getNumericValueDetails() {
        final String elevation = "DOCUMENT/ELEVATION";
        final String age = "DOCUMENT/AGE";
        final String notThere = "DOCUMENT/NOT_THERE";
        final IdolParametricRequest parametricRequest = mockRequest(Arrays.asList(elevation, age, notThere));

        final NumericValueDetails elevationValueDetails = NumericValueDetails.builder()
                .min(-50D)
                .max(1242D)
                .average(500.5)
                .sum(12314D)
                .totalValues(3)
                .build();
        final NumericValueDetails ageValueDetails = NumericValueDetails.builder()
                .min(0D)
                .max(96D)
                .average(26D)
                .sum(1314D)
                .totalValues(100)
                .build();
        final NumericValueDetails notThereValueDetails = NumericValueDetails.builder()
                .min(0D)
                .max(0D)
                .average(0D)
                .sum(0D)
                .totalValues(0)
                .build();

        final List<FlatField> responseFields = new LinkedList<>();
        responseFields.add(mockFlatField(elevation, elevationValueDetails, this::mockDateOrNumberJAXBElement));
        responseFields.add(mockFlatField(age, ageValueDetails, this::mockDateOrNumberJAXBElement));
        responseFields.add(mockFlatField(notThere, notThereValueDetails, this::mockDateOrNumberJAXBElement));

        final GetQueryTagValuesResponseData response = mock(GetQueryTagValuesResponseData.class);
        when(response.getField()).thenReturn(responseFields);

        when(queryExecutor.executeGetQueryTagValues(any(AciParameters.class), any())).thenReturn(response);

        final Map<FieldPath, NumericValueDetails> valueDetails = parametricValuesService.getNumericValueDetails(parametricRequest);
        assertThat(valueDetails.size(), is(3));
        assertThat(valueDetails, hasEntry(equalTo(tagNameFactory.getFieldPath(elevation)), equalTo(elevationValueDetails)));
        assertThat(valueDetails, hasEntry(equalTo(tagNameFactory.getFieldPath(age)), equalTo(ageValueDetails)));
        assertThat(valueDetails, hasEntry(equalTo(tagNameFactory.getFieldPath(notThere)), equalTo(notThereValueDetails)));
    }

    @Test
    public void getDateValueDetails() {
        final String date = "AUTN_DATE";
        final IdolParametricRequest parametricRequest = mockRequest(Collections.singletonList(date));

        final List<FlatField> responseFields = new LinkedList<>();
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        final DateValueDetails dateValueDetails = DateValueDetails.builder()
                .min(now)
                .max(now)
                .average(now)
                .sum(1234567D)
                .totalValues(1)
                .build();
        responseFields.add(mockFlatField(date, dateValueDetails, this::mockDateOrNumberJAXBElement));

        final GetQueryTagValuesResponseData response = mock(GetQueryTagValuesResponseData.class);
        when(response.getField()).thenReturn(responseFields);

        when(queryExecutor.executeGetQueryTagValues(any(AciParameters.class), any())).thenReturn(response);

        final Map<FieldPath, DateValueDetails> valueDetails = parametricValuesService.getDateValueDetails(parametricRequest);
        assertThat(valueDetails.size(), is(1));
        assertThat(valueDetails, hasEntry(equalTo(tagNameFactory.getFieldPath(date)), equalTo(dateValueDetails)));
    }

    @Test
    public void getNumericParametricValues() {
        mockBucketResponses(7,
                mockTagValue("1,2", 0),
                mockTagValue("2,3", 5),
                mockTagValue("3,4", 2),
                mockTagValue("4,5", 0),
                mockTagValue("5,6", 0)
        );

        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.singletonList("ParametricNumericDateField"));
        final List<RangeInfo> results = parametricValuesService.getNumericParametricValuesInBuckets(idolParametricRequest, ImmutableMap.of(tagNameFactory.getFieldPath("ParametricNumericDateField"), new BucketingParams(5, 1.0, 6.0)));
        assertThat(results, is(not(empty())));
        final RangeInfo info = results.iterator().next();
        final List<RangeInfo.Value> countInfo = info.getValues();
        assertEquals(7, info.getCount());
        assertEquals(1, info.getMin(), 0);
        assertEquals(6d, info.getMax(), 0);
        final Iterator<RangeInfo.Value> iterator = countInfo.iterator();
        assertEquals(new RangeInfo.Value(1, 2, 0), iterator.next());
        assertEquals(new RangeInfo.Value(2, 3, 5), iterator.next());
        assertEquals(new RangeInfo.Value(3, 4, 2), iterator.next());
        assertEquals(new RangeInfo.Value(4, 5, 0), iterator.next());
        assertEquals(new RangeInfo.Value(5, 6, 0), iterator.next());
    }

    @Test
    public void getRangesNoResults() {
        mockBucketResponses(0);

        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.singletonList("ParametricNumericDateField"));
        final List<RangeInfo> results = parametricValuesService.getNumericParametricValuesInBuckets(idolParametricRequest, ImmutableMap.of(tagNameFactory.getFieldPath("ParametricNumericDateField"), new BucketingParams(5, 1.0, 6.0)));
        assertThat(results, is(not(empty())));

        final RangeInfo info = results.iterator().next();
        final List<RangeInfo.Value> countInfo = info.getValues();
        final Iterator<RangeInfo.Value> iterator = countInfo.iterator();
        assertEquals(new RangeInfo.Value(1, 2, 0), iterator.next());
        assertEquals(new RangeInfo.Value(2, 3, 0), iterator.next());
        assertEquals(new RangeInfo.Value(3, 4, 0), iterator.next());
        assertEquals(new RangeInfo.Value(4, 5, 0), iterator.next());
        assertEquals(new RangeInfo.Value(5, 6, 0), iterator.next());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNumericParametricValuesZeroBucketsZeroBuckets() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.singletonList("ParametricNumericDateField"));
        parametricValuesService.getNumericParametricValuesInBuckets(idolParametricRequest, ImmutableMap.of(tagNameFactory.getFieldPath("ParametricNumericDateField"), new BucketingParams(0, 1.0, 5.0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNumericParametricValuesNoParams() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.singletonList("ParametricNumericDateField"));
        parametricValuesService.getNumericParametricValuesInBuckets(idolParametricRequest, Collections.emptyMap());
    }

    @Test
    public void bucketParametricValuesNoFields() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.emptyList());
        final List<RangeInfo> results = parametricValuesService.getNumericParametricValuesInBuckets(idolParametricRequest, Collections.emptyMap());
        assertThat(results, is(empty()));
    }

    @Test
    public void getDependentParametricValues() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.singletonList("Some field"));

        final GetQueryTagValuesResponseData responseData = mockRecursiveResponse();
        when(queryExecutor.executeGetQueryTagValues(any(AciParameters.class), any())).thenReturn(responseData);
        final Collection<DependentParametricField> results = parametricValuesService.getDependentParametricValues(idolParametricRequest);
        assertThat(results, is(not(empty())));
    }

    @Test
    public void getDependentValuesFieldNamesFirst() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.emptyList());

        final ImmutableMap<FieldTypeParam, Set<TagName>> fieldsResponse = ImmutableMap.of(FieldTypeParam.Parametric, Collections.singleton(tagNameFactory.buildTagName("CATEGORY")));
        when(fieldsService.getFields(any())).thenReturn(fieldsResponse);

        final GetQueryTagValuesResponseData responseData = mockRecursiveResponse();
        when(queryExecutor.executeGetQueryTagValues(any(AciParameters.class), any())).thenReturn(responseData);

        final Collection<DependentParametricField> results = parametricValuesService.getDependentParametricValues(idolParametricRequest);
        assertThat(results, is(not(empty())));
    }

    @Test
    public void dependentParametricValuesNotConfigured() {
        final IdolParametricRequest idolParametricRequest = mockRequest(Collections.emptyList());

        final Map<FieldTypeParam, Set<TagName>> response = ImmutableMap.of(FieldTypeParam.Parametric, Collections.emptySet());
        when(fieldsService.getFields(any())).thenReturn(response);

        final Collection<DependentParametricField> results = parametricValuesService.getDependentParametricValues(idolParametricRequest);
        assertThat(results, is(empty()));
    }

    private IdolParametricRequest mockRequest(final Collection<String> fieldNames) {
        final IdolParametricRequest parametricRequest = mock(IdolParametricRequest.class);
        final List<FieldPath> paths = fieldNames.stream().map(tagNameFactory::getFieldPath).collect(Collectors.toList());
        when(parametricRequest.getFieldNames()).thenReturn(paths);

        final IdolParametricRequestBuilder parametricRequestBuilder = mock(IdolParametricRequestBuilder.class);
        when(parametricRequestBuilder.maxValues(any())).thenReturn(parametricRequestBuilder);
        when(parametricRequestBuilder.ranges(any())).thenReturn(parametricRequestBuilder);
        when(parametricRequestBuilder.start(any())).thenReturn(parametricRequestBuilder);
        when(parametricRequestBuilder.sort(any())).thenReturn(parametricRequestBuilder);
        when(parametricRequest.toBuilder()).thenReturn(parametricRequestBuilder);
        when(parametricRequestBuilder.build()).thenReturn(parametricRequest);

        return parametricRequest;
    }

    private GetQueryTagValuesResponseData mockQueryResponse() {
        final GetQueryTagValuesResponseData responseData = new GetQueryTagValuesResponseData();
        final FlatField field = new FlatField();
        field.setTotalValues(5);
        field.getName().add("Some name");
        when(element.getName()).thenReturn(new QName("", IdolParametricValuesServiceImpl.VALUE_NODE_NAME));
        final TagValue tagValue = mockTagValue("Some field", 5);
        when(element.getValue()).thenReturn(tagValue);
        field.getValueAndSubvalueOrValues().add(element);
        responseData.getField().add(field);
        return responseData;
    }

    private <T extends Comparable<? super T>, V extends ValueDetails<T>> FlatField mockFlatField(final String name,
                                                                                                 final V valueDetails,
                                                                                                 final BiFunction<String, ? super T, JAXBElement<Serializable>> generateElement
    ) {
        final FlatField flatField = mock(FlatField.class);
        when(flatField.getName()).thenReturn(Collections.singletonList(name));
        when(flatField.getTotalValues()).thenReturn(valueDetails.getTotalValues());

        final List<JAXBElement<? extends Serializable>> values = new LinkedList<>();
        values.add(generateElement.apply(IdolParametricValuesServiceImpl.VALUE_MIN_NODE_NAME, valueDetails.getMin()));
        values.add(generateElement.apply(IdolParametricValuesServiceImpl.VALUE_MAX_NODE_NAME, valueDetails.getMax()));
        values.add(generateElement.apply(IdolParametricValuesServiceImpl.VALUE_AVERAGE_NODE_NAME, valueDetails.getAverage()));
        values.add(mockJAXBElement(IdolParametricValuesServiceImpl.VALUE_SUM_NODE_NAME, valueDetails.getSum()));

        when(flatField.getValueAndSubvalueOrValues()).thenReturn(values);
        return flatField;
    }

    private JAXBElement<Serializable> mockDateOrNumberJAXBElement(final String name, final ChronoZonedDateTime<?> date) {
        return mockDateOrNumberJAXBElement(name, date.toEpochSecond(), date);
    }

    private JAXBElement<Serializable> mockDateOrNumberJAXBElement(final String name, final Double number) {
        return mockDateOrNumberJAXBElement(name, number, null);
    }

    private JAXBElement<Serializable> mockDateOrNumberJAXBElement(final String name, final double number, final TemporalAccessor date) {
        @SuppressWarnings("unchecked") final JAXBElement<Serializable> jaxbElement = mock(JAXBElement.class);

        when(jaxbElement.getName()).thenReturn(new QName("", name));
        final DateOrNumber dateOrNumber = new DateOrNumber();
        if (date != null) {
            dateOrNumber.setDate(IdolParametricValuesService.DATE_FORMAT.format(date));
        }
        dateOrNumber.setValue(number);
        when(jaxbElement.getValue()).thenReturn(dateOrNumber);
        return jaxbElement;
    }

    private JAXBElement<Serializable> mockJAXBElement(final String name, final double value) {
        @SuppressWarnings("unchecked")
        final JAXBElement<Serializable> jaxbElement = mock(JAXBElement.class);

        when(jaxbElement.getName()).thenReturn(new QName("", name));
        when(jaxbElement.getValue()).thenReturn(value);
        return jaxbElement;
    }

    private void mockBucketResponses(final int count, final TagValue... tagValues) {
        when(element.getName()).thenReturn(
                new QName("", IdolParametricValuesServiceImpl.VALUES_NODE_NAME),
                new QName("", IdolParametricValuesServiceImpl.VALUE_NODE_NAME)
        );

        OngoingStubbing<Serializable> stub = when(element.getValue()).thenReturn(count);

        for (final TagValue tagValue : tagValues) {
            stub = stub.thenReturn(tagValue);
        }

        final GetQueryTagValuesResponseData responseData = new GetQueryTagValuesResponseData();
        final FlatField field2 = new FlatField();
        field2.getName().add("ParametricNumericDateField");
        field2.getValueAndSubvalueOrValues().add(element);
        for (final TagValue ignored : tagValues) {
            field2.getValueAndSubvalueOrValues().add(element);
        }
        responseData.getField().add(field2);

        when(queryExecutor.executeGetQueryTagValues(any(AciParameters.class), any())).thenReturn(responseData);
    }

    private TagValue mockTagValue(final String value, final int count) {
        final TagValue tagValue = new TagValue();
        tagValue.setValue(value);
        tagValue.setCount(count);
        return tagValue;
    }

    private GetQueryTagValuesResponseData mockRecursiveResponse() {
        final GetQueryTagValuesResponseData responseData = new GetQueryTagValuesResponseData();

        final FlatField fieldNames = new FlatField();
        fieldNames.getName().add("ParametricField");
        responseData.getField().add(fieldNames);

        final RecursiveField field = new RecursiveField();
        field.setValue("Some field");
        field.setCount(5);
        final Values values = new Values();
        values.getField().add(field);
        responseData.setValues(values);

        return responseData;
    }
}
