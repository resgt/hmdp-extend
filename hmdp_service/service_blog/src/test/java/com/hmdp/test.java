package com.hmdp;

import java.util.HashSet;

public class test {

    public static void main(String[] args) {
        HashSet<String> strings = new HashSet<>();

        strings.add("1");
        strings.add("2");


        for (String string : strings) {
            Long id = Long.valueOf(string);
            System.out.println(id);
        }
    }

}
