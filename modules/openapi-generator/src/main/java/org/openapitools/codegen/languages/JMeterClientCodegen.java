/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenSecurity;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JMeterClientCodegen extends DefaultCodegen implements CodegenConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMeterClientCodegen.class);

    // source folder where to write the files
    protected String sourceFolder = "";

    protected String apiVersion = "1.0.0";

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see org.openapitools.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -g flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return "jmeter";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates a JMeter .jmx file.";
    }

    public JMeterClientCodegen() {
        super();

        // set the output folder here
        outputFolder = "generated-code/JMeterClientCodegen";

        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "api.mustache",   // the template to use
                ".jmx");       // the extension for each file to write

        apiTemplateFiles.put("testdata-localhost.mustache", ".csv");
        apiTemplateFiles.put("start-load-test.mustache", ".sh");

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        embeddedTemplateDir = templateDir = "jmeter-client";

        /*
         * Api Package.  Optional, if needed, this can be used in templates
         */
        apiPackage = "";

        /*
         * Model Package.  Optional, if needed, this can be used in templates
         */
        modelPackage = "";

        /*
         * Reserved words.  Override this with reserved words specific to your language
         */
        reservedWords = new HashSet<String>(
                Arrays.asList(
                        "sample1",  // replace with static values
                        "sample2")
        );

        /*
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("apiVersion", apiVersion);
        // supportingFiles.add(new SupportingFile("testdata-localhost.mustache", "input", "testdata-localhost.csv"));
    }

    @Override
    public String setParameterExampleValueFromContentValueExample(Object example) {
        if (example instanceof Map) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(example);
            } catch (JsonProcessingException e) {
                LOGGER.warn("Can not convert map to JSON of this value:" + example.toString(), e);
            }
        }
        return super.setParameterExampleValueFromContentValueExample(example);
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        if (openAPI != null && openAPI.getPaths() != null) {
            for (String pathname : openAPI.getPaths().keySet()) {
                PathItem path = openAPI.getPaths().get(pathname);
                if (path.readOperations() != null) {
                    for (Operation operation : path.readOperations()) {
                        String pathWithDollarsAndOperationId =
                                pathname.replaceAll("\\{", "\\$\\{" + operation.getOperationId() + "_");
                        operation.addExtension("x-path", pathWithDollarsAndOperationId);
                    }
                }
            }
        }
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    /**
     * Location to write model files.  You can use the modelPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    /**
     * Prevent any changing of parameters
     *
     * @param name Codegen property object
     * @return same name
     */
    @Override
    public String toParamName(String name) {
        return name;
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
    }

    /**
     * Optional - type declaration.  This is a String which is used by the templates to instantiate your
     * types.  There is typically special handling for different property types
     *
     * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isArraySchema(p)) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            return getSchemaType(p) + "[" + getTypeDeclaration(inner) + "]";
        } else if (ModelUtils.isMapSchema(p)) {
            Schema inner = ModelUtils.getAdditionalProperties(p);
            return getSchemaType(p) + "[String, " + getTypeDeclaration(inner) + "]";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public CodegenSecurity postProcessEachCodegenSecurity(CodegenSecurity cs, SecurityScheme securityScheme) {
        if (cs.keyParamName.startsWith("Authorization")) {
            cs.hasDefaultValue = true;
            cs.defaultValue = "Bearer ${access_token}";
        }
        return cs;
    }

    /**
     * Optional - OpenAPI type conversion.  This is used to map OpenAPI types in a `Schema` into
     * either language specific types via `typeMapping` or into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     */
    @Override
    public String getSchemaType(Schema p) {
        String openAPIType = super.getSchemaType(p);
        String type = null;
        if (typeMapping.containsKey(openAPIType)) {
            type = typeMapping.get(openAPIType);
            if (languageSpecificPrimitives.contains(type)) {
                return toModelName(type);
            }
        } else {
            type = openAPIType;
        }
        return toModelName(type);
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove ' to avoid code injection
        return input.replace("'", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @Override
    public void postProcessParameter(CodegenParameter codegenParameter) {
        if (codegenParameter.isHeaderParam) {
            if (codegenParameter.baseName.equals("Authorization")) {
                codegenParameter.isAuthorizationParam = true;
                codegenParameter.value = "Bearer ${access_token}";
            } else if (codegenParameter.baseName.equals("Content-Type")) {
                codegenParameter.isContentType = true;
            }
        }
        if (codegenParameter.value != null) {
            codegenParameter.hasValue = true;
        }
        if (codegenParameter.example != null) {
            codegenParameter.hasExample = true;
        }

    }

    @Override
    protected Set<String> loadIgnoringParameters() {
        HashSet<String> ignoreParameters = new HashSet<>();
        ignoreParameters.add("cursor");
        ignoreParameters.add("size");
        ignoreParameters.add("name");
        ignoreParameters.add("sort");
        ignoreParameters.add("id");
//        fileName = "ignoring-parameters.txt";
//        File file = new File(fileName);
//        if (!file.exists()) {
//            LOGGER.warn("File "+ fileName+" not exists ");
//            return ignoreParameters;
//        }
//        try {
//            Scanner scanner = new Scanner(file);
//            if (scanner.hasNext()) {
//                ignoreParameters.add(scanner.nextLine());
//            }
//        } catch (FileNotFoundException e) {
//            LOGGER.warn("Can not obtain file "+ fileName, e);
//            return ignoreParameters;
//        }
        return ignoreParameters;
    }
}
