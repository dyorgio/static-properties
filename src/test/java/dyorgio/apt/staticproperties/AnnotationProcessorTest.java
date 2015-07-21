package dyorgio.apt.staticproperties;

import com.google.testing.compile.JavaFileObjects;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import javax.xml.bind.annotation.XmlRootElement;
import org.junit.Test;

public class AnnotationProcessorTest {

    private static final String[] ORIGINAL_SOURCE = {
        "import javax.xml.bind.annotation.XmlRootElement;",//
        "import java.util.List;",//
        "",//
        "@XmlRootElement",//
        "public class TestClass {",//
        "",//
        "    private TestClass child;",//
        "    private String string;",//
        "    private List<TestClass> children;",//
        "    private List<String> strings;",//
        "}"
    };

    private static final String[] EXPECTED_SOURCE = {
        "import dyorgio.apt.staticproperties.StaticProperty;",//
        "import dyorgio.apt.staticproperties.StaticListProperty;",//
        "",//
        "public class DTestClass extends StaticProperty {",//
        "",//
        "	public static final DTestClass testClass = new DTestClass();",//
        "",//
        "	public DTestClass(StaticProperty parent, String path) {super(parent,path);}",//
        "	private DTestClass() {super();}",//
        "",//
        "	public final DTestClass child = new DTestClass(this, \"child\");",//
        "	public final StaticProperty string = new StaticProperty(this, \"string\");",//
        "	public final StaticListProperty<DTestClass> children = new StaticListProperty(this, \"children\"){public DTestClass get(int index) { return new DTestClass(this, String.valueOf(index));}};",//
        "	public final StaticListProperty<StaticProperty> strings = new StaticListProperty(this, \"strings\"){public StaticProperty get(int index) { return new StaticProperty(this, String.valueOf(index));}};",//
        "}"
    };

    @Test
    public void testCompilation() {
        assertAbout(javaSource())
                .that(JavaFileObjects.forSourceLines("TestClass", ORIGINAL_SOURCE))
                .withCompilerOptions("-A" + AnnotationProcessor.STATICPROPERTIES_ANNOTATIONS + "=" + XmlRootElement.class.getName())
                .processedWith(new AnnotationProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceLines("DTestClass", EXPECTED_SOURCE));
    }
}
