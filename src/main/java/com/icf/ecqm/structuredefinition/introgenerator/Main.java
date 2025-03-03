package com.icf.ecqm.structuredefinition.introgenerator;

import com.icf.ecqm.structuredefinition.introgenerator.deqm.DEQMProcessor;
import com.icf.ecqm.structuredefinition.introgenerator.qicore.QICoreProcessor;

public class Main {

    public static void main(String[] args) {
        boolean MS_ARG = false;
        boolean deqm = false;
        boolean qicore = false;
        for (String arg : args) {
            if (arg.contains("ms")) {
                MS_ARG = true;
                continue;
            }

            if(arg.contains("qi-core") || arg.contains("qicore")){
                qicore = true;
                continue;
            }else if(arg.contains("deqm")){
                deqm = true;
                continue;
            }
        }

        if (qicore) {
            new QICoreProcessor().runProcessor();
        }else if (deqm){
            new DEQMProcessor().runProcessor();
        }else{
            System.out.println ("Please specify 'deqm' or 'qi-core' as an argument when calling this jar");
        }
    }

}