package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = SeckillVouchersApp.class)
class ShopNewCacheTests {

    @Autowired
    private ShopServiceImpl shopServiceImpl;

    @Test
    void shopCacheTest() throws InterruptedException {


    }


}
