
import com.litevar.wechat.adapter.WechatAdapterServerApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author Teoan
 * @since 2025/8/27 09:25
 */
@SpringBootTest(classes = WechatAdapterServerApplication.class)
@Slf4j
public class WechatAdapterServerTest {


    @Test
    void test(){
         StringBuilder chunkContent = new StringBuilder();
         chunkContent.append("我");
         chunkContent.append("\n");
         chunkContent.append("\n");
         chunkContent.append("你");
        log.info(chunkContent.toString());


    }




}
