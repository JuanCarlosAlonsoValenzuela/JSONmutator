package es.us.isa.jsonmutator.experiment2.generateAssertions;

import es.us.isa.jsonmutator.experiment2.readTestCases.TestCase;
import org.json.simple.JSONObject;

import java.util.*;

import static es.us.isa.jsonmutator.experiment2.readTestCases.JSONManager.stringToJsonObject;


public class DeclsVariable {

    public static String getEnterParameterValue(TestCase testCase, List<String> hierarchy) {
        Map<String, String> queryParametersValues = testCase.getQueryParameters();
        Map<String, String> pathParametersValues = testCase.getPathParameters();
        Map<String, String> headerParametersValues = testCase.getHeaderParameters();
        Map<String, String> formParametersValues = testCase.getFormParameters();
        String bodyParameter = testCase.getBodyParameter();

        String value = null;

        String key = hierarchy.get(hierarchy.size()-1);
        value = queryParametersValues.get(key);
        if(value == null) {
            value = pathParametersValues.get(key);
        }
        if(value == null) {
            value = headerParametersValues.get(key);
        }
        if(value == null) {
            value = formParametersValues.get(key);
        }

        // Search in body parameter
        if(value == null &&  bodyParameter != null && !bodyParameter.equals("")) {
            JSONObject jsonBodyParameter = stringToJsonObject(bodyParameter);
            if(jsonBodyParameter != null) {
                List<String> hierarchyBody = hierarchy.subList(1, hierarchy.size());
                value = getPrimitiveValueFromHierarchy(jsonBodyParameter, hierarchyBody);
            }
        }

        // Set value to null if its value should be considered as null
//        if(Arrays.asList(stringsToConsiderAsNull).contains(value)) {
//            value = null;
//        }


        return value;

    }


    public static String getPrimitiveValueFromHierarchy(JSONObject json, List<String> hierarchy) {
        String key = hierarchy.get(0);

        if(hierarchy.size() == 1) {

            if(json.get(key) == null){
                return null;
            } else {
                return String.valueOf(json.get(key));
            }

        } else {
            Object jsonSon = json.get(key);

            if(jsonSon instanceof JSONObject) {     // If JSONObject
                JSONObject jsonSonObject = (JSONObject) jsonSon;
                return getPrimitiveValueFromHierarchy(jsonSonObject, hierarchy.subList(1, hierarchy.size()));
            } else {    // If JSONArray
                // TODO: Complete
                return null;
            }
        }

    }





}
