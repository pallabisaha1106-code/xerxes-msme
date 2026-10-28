package com.xerxes.msme.config;

import com.xerxes.msme.admin.DefaultCatalogProperties;
import com.xerxes.msme.catalog.ProductImageProperties;
import com.xerxes.msme.security.JwtProperties;
import com.xerxes.msme.storage.GcsProperties;
import com.xerxes.msme.storage.StorageProperties;
import com.xerxes.msme.storefront.StorefrontProperties;
import com.xerxes.msme.subscription.RazorpayProperties;
import com.xerxes.msme.subscription.SubscriptionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    CorsProperties.class,
    StorageProperties.class,
    StorefrontProperties.class,
    RazorpayProperties.class,
    SubscriptionProperties.class,
    GcsProperties.class,
    DefaultCatalogProperties.class,
    ProductImageProperties.class
})
public class PropertiesConfig {}
