package com.example.basics;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@SpringBootApplication
@Import(MyBeanRegistrar.class)
public class BasicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicsApplication.class, args);
    }

}

class MyBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {


        for (var i = 0; i < 100; i++) {
            var indx = i;
            registry.registerBean("instance" + i, MyInstance.class,
                    myInstanceSpec -> myInstanceSpec.supplier(
                            _ -> new MyInstance("instance" + indx)));
        }

        registry.registerBean(Baz.class);

        registry.registerBean(Foo.class, p -> p
                .supplier(supplierContext -> new Foo(supplierContext.bean("myBar1", Bar.class)))
                .description("my deascription")
                );
    }
}

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Qualifier("devoxxBar")
@Documented
@interface DevoxxBean {
    String value() default "";
}

class MyInstance {
    String instance;

    MyInstance(String instance) {
        this.instance = instance;
        System.out.println("instance:" + this.instance);
    }
}

@Configuration
class MyOtherConfig {

//    @Bean
//    Foo foo(MyConfig myConfig) {
//
////        for (var i = 0; i < 100; i++)
////            this.bar0() ;
//
//        for (var i = 0; i < 100; i++)
//            myConfig.bar0(null);
//
//        var myBar = myConfig.bar0(null);
//
//        return new Foo(myBar);
//    }

}

//@ComponentScan
@Configuration
class MyConfig {


    @Bean(name = "myBar1")
    Bar bar1() {
        return new Bar();
    }

    @DevoxxBean
    @Bean
//    @Scope("prototype")
    Bar bar0(Environment environment) {
        var version = environment.getProperty("java.version");
        System.out.println("the version is " + version);
        System.out.println("Creating bar0");
        return new Bar();
    }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@interface DevoxxService {

    /**
     * Alias for {@link Component#value}.
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

}


//@DevoxxService
class Baz {

    Baz() {
        System.out.println("Baz!");
    }
}

class Foo {

    private final Bar bar;

    Foo(Bar bar) {
        this.bar = bar;
    }
}

class Bar {
}