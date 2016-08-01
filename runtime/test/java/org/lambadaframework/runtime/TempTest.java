package org.lambadaframework.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by arran on 7/07/2016.
 */
public class TempTest {
    @Test
    public void testConvert()
            throws Exception {

        String[] input = {"Hi", "hello"};
        Collection<String> input2 = Arrays.asList(input);

        ObjectMapper objectMapper = new ObjectMapper();

        Set<String> output = new HashSet<String>();
        String[] output2;
        String output3= null;

//        output2 = objectMapper.convertValue(input2, String[].class);
//        output2 = objectMapper.convertValue(input2, String[].class);
        output3 = objectMapper.convertValue(input2, String.class);

//        Assert.assertTrue("Output is rightsized.", output2.length == 2);

        Assert.assertTrue("Output has a value", output3 != null);

    }

}
