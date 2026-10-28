/**
 * Guest storefront orders: a public, unauthenticated read of the catalog and
 * write of an order request, plus the authenticated admin inbox to review,
 * confirm (creates a Customer + Invoice) or reject each one. No payment is
 * ever collected here.
 */
package com.xerxes.msme.storefront;
