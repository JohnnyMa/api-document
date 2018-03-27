package com.github.liuanxin.api.util;

import com.github.liuanxin.api.annotation.ApiParam;
import com.github.liuanxin.api.annotation.ApiParamIgnore;
import com.github.liuanxin.api.model.DocumentParam;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class ParamHandler {

    private static final LocalVariableTableParameterNameDiscoverer VARIABLE
            = new LocalVariableTableParameterNameDiscoverer();

    public static List<DocumentParam> handlerParam(HandlerMethod handlerMethod) {
        List<DocumentParam> params = new ArrayList<>();
        MethodParameter[] methodParameters = handlerMethod.getMethodParameters();
        for (int i = 0; i < methodParameters.length; i++) {
            MethodParameter parameter = methodParameters[i];
            // if param not basicType, into a layer of processing
            Class<?> parameterType = parameter.getParameterType();
            if (Tools.notBasicType(parameterType)) {
                for (Field field : parameterType.getDeclaredFields()) {
                    int mod= field.getModifiers();
                    // field not static, not final, not annotation ignore
                    if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)
                            && Tools.isBlank(field.getAnnotation(ApiParamIgnore.class))) {
                        String paramName = field.getName();
                        params.add(paramInfo(paramName, field.getType(), field.getAnnotation(ApiParam.class)));
                    }
                }
            } else {
                if (Tools.isBlank(parameter.getParameterAnnotation(ApiParamIgnore.class))) {
                    // The variable name is erased when compiled by jvm, parameter.parameterName() is null
                    // if use java 8 and open options in javac -parameters, parameter.parameterName() can be get
                    // String paramName = parameter.getParameterName();
                    String paramName = VARIABLE.getParameterNames(parameter.getMethod())[i];
                    params.add(paramInfo(paramName, parameterType, parameter.getParameterAnnotation(ApiParam.class)));
                }
            }
        }
        return params;
    }

    /** collect param info */
    private static DocumentParam paramInfo(String name, Class<?> type, ApiParam apiParam) {
        DocumentParam param = new DocumentParam();
        param.setName(name);
        param.setDataType(Tools.getInputType(type));

        if (Tools.isNotBlank(apiParam)) {
            String desc = apiParam.desc();
            if (Tools.isNotBlank(desc)) {
                param.setDesc(desc);
            }

            String paramName = apiParam.name();
            if (Tools.isNotBlank(paramName)) {
                param.setName(paramName);
            }
            String dataType = apiParam.dataType();
            if (Tools.isNotBlank(dataType)) {
                param.setDataType(apiParam.dataType());
            }

            param.setParamType(apiParam.paramType().toString());

            String example = apiParam.example();
            if (Tools.isNotBlank(example)) {
                param.setExample(example);
            }

            param.setMust(apiParam.must());
        }
        if (type.isEnum()) {
            // enum append (code:value)
            String desc = param.getDesc();
            String enumInfo = Tools.enumInfo(type);
            param.setDesc(Tools.isBlank(desc) ? enumInfo : (desc + "(" + enumInfo + ")"));
        }
        return param;
    }
}
