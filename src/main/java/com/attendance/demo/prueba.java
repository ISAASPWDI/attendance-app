package com.attendance.demo;

public class prueba {

    private String id;
    private String email;
    private String pass;

    public static String testingMethods(){
        return "Hello world from java spring boot";
    }
    prueba(){

    }
    prueba(String id, String email, String pass){
        this.id = id;
        this.email = email;
        this.pass = pass;
    }
    public String printObject(){
        return "El id es: " + this.id + ", email entered :" + this.email;
    }

    public static void main(String[] args) {
        System.out.println(prueba.testingMethods());
        var newTestObject = new prueba("abc", "asd@asd.com", "12345");
        System.out.println(newTestObject.printObject());
    }
}
