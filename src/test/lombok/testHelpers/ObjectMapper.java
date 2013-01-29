package testHelpers;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;

import java.lang.reflect.ParameterizedType;

public class ObjectMapper<T> {

    private static Mapper mapper = new DozerBeanMapper();
    final private Class<T> entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    public T build() {
        return mapper.map(this, entityType);
    }
}
