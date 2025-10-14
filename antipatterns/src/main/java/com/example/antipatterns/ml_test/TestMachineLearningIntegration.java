package com.example.antipatterns.ml_test;

@SuppressWarnings("unused")
public class TestMachineLearningIntegration {

    /**
     * Some random JavaDoc comment
     *
     * @param interestingParam some param
     */
    public void testMethodWithCorrectComment(String interestingParam) {
        // Correct comment
        System.out.println("Some print");
    }

    /**
     * <Return label 1 comment> return-label-1
     * @param interestingParam return-label-1
     */
    public void testMethodWithLabel1(String interestingParam) {
        System.out.println("Some print");
        // <Return label 1 comment> return-label-1

        System.out.println("Some print 2");
        /* Some block comment
            <Return label 1 comment>
            return-label-1
        */
    }

    /**
     * Some random JavaDoc comment return-label-2
     * @param interestingParam return-label-2
     */
    public void testMethodWithLabel2(String interestingParam) {
        System.out.println("Some print");
        // Incorrect comment return-label-2

        System.out.println("Some print 2");
        /* Some block comment
            return-label-2
        */
    }




}
