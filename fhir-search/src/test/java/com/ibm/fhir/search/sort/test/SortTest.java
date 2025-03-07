/*
 * (C) Copyright IBM Corp. 2016, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.search.sort.test;

import static com.ibm.fhir.model.type.String.string;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.resource.SearchParameter;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Markdown;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.model.type.code.PublicationStatus;
import com.ibm.fhir.model.type.code.ResourceType;
import com.ibm.fhir.model.type.code.SearchParamType;
import com.ibm.fhir.search.context.FHIRSearchContext;
import com.ibm.fhir.search.context.FHIRSearchContextFactory;
import com.ibm.fhir.search.exception.FHIRSearchException;
import com.ibm.fhir.search.sort.Sort;
import com.ibm.fhir.search.test.BaseSearchTest;
import com.ibm.fhir.search.util.SearchUtil;

/**
 * This unit test class contains methods that test the parsing of sort
 * parameters in the Sort and Sort.Direction class.
 */
public class SortTest extends BaseSearchTest {

    final static Sort sort = new Sort();

    /**
     * Tests an invalid direction modifier on the _sort query parameter.
     * This now throws as it's invalid parameter name-value
     *
     * @throws Exception
     */
    @Test(expectedExceptions = FHIRSearchException.class)
    public void testInvalidDirection() throws Exception {
        Map<String, List<String>> queryParameters = new HashMap<>();
        Class<Patient> resourceType = Patient.class;

        queryParameters.put("_sort:xxx", Collections.singletonList("birthdate"));
        SearchUtil.parseQueryParameters(resourceType, queryParameters);
    }

    @Test
    public void testExceptionMessages() throws Exception {
        String msg = sort.buildUndefinedSortParamMessage("resourceTypeName", "sortParmCode");
        assertEquals(msg, "Undefined sort parameter. resourceType=[resourceTypeName] sortParmCode=[sortParmCode]");

        msg = sort.buildNewInvalidSearchExceptionMessage("sortParmCode");
        assertEquals(msg, "[sortParmCode] is not supported as a sort parameter on a system-level search.");
    }

    @Test(expectedExceptions = {})
    public void testCheckSystemLevelWithNonSystem() throws Exception {
        sort.checkSystemLevel("Patient", "sortParmCode");
        assertTrue(true);
    }

    @Test(expectedExceptions = { FHIRSearchException.class })
    public void testCheckSystemLevelWithResource() throws Exception {
        sort.checkSystemLevel("Resource", "sortParmCode");
    }

    @Test(expectedExceptions = {})
    public void testCheckSystemLevelWithResourceId() throws Exception {
        sort.checkSystemLevel("Resource", "_id");
        assertTrue(true);
    }

    @Test(expectedExceptions = {})
    public void testCheckSystemLevelWithResourceLastUpdated() throws Exception {
        sort.checkSystemLevel("Resource", "_lastUpdated");
        assertTrue(true);
    }

    @Test(expectedExceptions = { FHIRSearchException.class })
    public void testIsUndefinedOrLenientAsNoSPAndNotLenient() throws Exception {
        String resourceTypeName = "TestResource";
        String sortParmCode = "LoggingCode";
        SearchParameter sortParmProxy = null;
        boolean lenient = false;

        assertTrue(sort.isUndefinedOrLenient(resourceTypeName, sortParmCode, sortParmProxy, lenient));
    }

    @Test(expectedExceptions = {})
    public void testIsUndefinedOrLenientAsNoSPAndLenient() throws Exception {
        String resourceTypeName = "TestResource";
        String sortParmCode = "LoggingCode";
        SearchParameter sortParmProxy = null;
        boolean lenient = true;

        assertTrue(sort.isUndefinedOrLenient(resourceTypeName, sortParmCode, sortParmProxy, lenient));
    }

    @Test(expectedExceptions = {})
    public void testIsUndefinedOrLenientAsLenient() throws Exception {
        String resourceTypeName = "TestResource";
        String sortParmCode = "LoggingCode";
        SearchParameter sortParmProxy =
                SearchParameter.builder().url(Uri.of("test")).name(string("test")).status(PublicationStatus.DRAFT)
                        .description(Markdown.of("test")).code(Code.of("test"))
                        .base(Arrays.asList(ResourceType.ACCOUNT)).type(SearchParamType.NUMBER)
                        .expression(string("test")).build();

        boolean lenient = true;
        assertFalse(sort.isUndefinedOrLenient(resourceTypeName, sortParmCode, sortParmProxy, lenient));

        lenient = false;
        assertFalse(sort.isUndefinedOrLenient(resourceTypeName, sortParmCode, sortParmProxy, lenient));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testSortDirectionNull() throws Exception {
        Sort.Direction.fromValue(null);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testSortDirectionEmpty() throws Exception {
        Sort.Direction.fromValue("");
    }

    @Test(expectedExceptions = {})
    public void testSortDirection() throws Exception {
        Sort.Direction down = Sort.Direction.fromValue("-_id");
        Sort.Direction up = Sort.Direction.fromValue("_id");

        assertEquals(Sort.Direction.DECREASING.compareTo(down), 0);
        assertEquals(Sort.Direction.INCREASING.compareTo(up), 0);
    }

    @Test(expectedExceptions = {})
    public void testParseSortParameterAsc() throws Exception {
        Class<?> resourceType = Patient.class;
        FHIRSearchContext context = FHIRSearchContextFactory.createSearchContext();
        String sortQueryParmValue = "_id";
        boolean lenient = false;

        sort.parseSortParameter(resourceType, context, sortQueryParmValue, lenient);
        assertEquals(context.getSortParameters().size(), 1);
        assertEquals(context.getSortParameters().get(0).getDirection().value(), '+');
    }

    @Test(expectedExceptions = {})
    public void testParseSortParameterDesc() throws Exception {
        Class<?> resourceType = Patient.class;
        FHIRSearchContext context = FHIRSearchContextFactory.createSearchContext();
        String sortQueryParmValue = "-_id";
        boolean lenient = false;

        sort.parseSortParameter(resourceType, context, sortQueryParmValue, lenient);
        assertEquals(context.getSortParameters().size(), 1);
        assertEquals(context.getSortParameters().get(0).getDirection().value(), '-');
    }

    @Test(expectedExceptions = {})
    public void testParseSortParameterResourceDesc() throws Exception {
        Class<?> resourceType = Resource.class;
        FHIRSearchContext context = FHIRSearchContextFactory.createSearchContext();
        String sortQueryParmValue = "-_id";
        boolean lenient = false;

        sort.parseSortParameter(resourceType, context, sortQueryParmValue, lenient);
        assertEquals(context.getSortParameters().size(), 1);
        assertEquals(context.getSortParameters().get(0).getDirection().value(), '-');
    }

    @Test(expectedExceptions = {FHIRSearchException.class})
    public void testParseSortParameterResourceBad() throws Exception {
        Class<?> resourceType = Observation.class;
        FHIRSearchContext context = FHIRSearchContextFactory.createSearchContext();
        String sortQueryParmValue = "-component-code2";
        boolean lenient = false;
        sort.parseSortParameter(resourceType, context, sortQueryParmValue, lenient);
    }

    @Test(expectedExceptions = {})
    public void testParseSortParameterResourceBadAsLenient() throws Exception {
        Class<?> resourceType = Observation.class;
        FHIRSearchContext context = FHIRSearchContextFactory.createSearchContext();
        String sortQueryParmValue = "-component-code2";
        boolean lenient = true;
        sort.parseSortParameter(resourceType, context, sortQueryParmValue, lenient);

        assertTrue(context.getSortParameters().isEmpty());
    }
}