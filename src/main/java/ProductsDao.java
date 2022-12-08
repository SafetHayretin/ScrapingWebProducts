import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class ProductsDao {
    SqlSession session;

    public ProductsDao() {
        this.session = createSession();
    }

    private SqlSession createSession() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
        } catch (IOException e) {
            System.out.println("Unable to create session");
        }

        return session;
    }

    public int insert(Product product) {
        int id = session.insert("Product.insert", product);
        session.commit();

        return id;
    }

    public int update(Product product) {
        int id = session.update("Product.update", product);
        session.commit();

        return id;
    }

    public int delete(Product product) {
        int id = session.delete("Product.deleteById", product);
        session.commit();

        return id;
    }

    public Product get(Integer id) {
        Product post = session.selectOne("Models.Product.selectById", id);
        session.commit();

        return post;
    }

    public List<Product> getAll() {
        List<Product> products = session.selectList("Product.getAll");
        session.commit();

        return products;
    }
}
