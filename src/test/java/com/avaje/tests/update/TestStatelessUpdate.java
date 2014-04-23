package com.avaje.tests.update;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.avaje.ebean.BaseTestCase;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.tests.model.basic.Contact;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.EBasic;
import com.avaje.tests.model.basic.EBasic.Status;

public class TestStatelessUpdate extends BaseTestCase {

  private EbeanServer server;

  @Before
  public void setUp() {
    server = Ebean.getServer(null);
  }

  @Test
  public void test() {

    // GlobalProperties.put("ebean.defaultUpdateNullProperties", "true");
    // GlobalProperties.put("ebean.defaultDeleteMissingChildren", "false");

    EBasic e = new EBasic();
    e.setName("something");
    e.setStatus(Status.NEW);
    e.setDescription("wow");

    server.save(e);

    EBasic updateAll = new EBasic();
    updateAll.setId(e.getId());
    updateAll.setName("updAllProps");

    server.update(updateAll, null, false);

    EBasic updateDeflt = new EBasic();
    updateDeflt.setId(e.getId());
    updateDeflt.setName("updateDeflt");

    server.update(updateDeflt);

  }

  /**
   * I am expecting that Ebean detects there aren't any changes and don't execute any query.
   * Currently a {@link javax.persistence.PersistenceException} with message 'Invalid value "null" for parameter "SQL"' is thrown.
   */
  @Test
  public void testWithoutChangesAndIgnoreNullValues() {
    // arrange
    EBasic basic = new EBasic();
    basic.setName("something");
    basic.setStatus(Status.NEW);
    basic.setDescription("wow");

    server.save(basic);

    // act
    EBasic basicWithoutChanges = new EBasic();
    basicWithoutChanges.setId(basic.getId());
    server.update(basicWithoutChanges);

    // assert
    // Nothing to check, simply no exception should occur
    // maybe ensure that no update has been executed
  }

  /**
   * Nice to have:
   * <br />
   * Assuming we have a Version column, it will always be generated an Update despite we have nothing to update.
   * It would be nice that this would be recognized and no update would happen.
   * <br />
   * <br />
   * This feature already works for normal Updates!
   * <br />
   * see: {@link com.avaje.tests.update.TestUpdatePartial#testWithoutChangesAndVersionColumn()}
   */
  @Test
  public void testWithoutChangesAndVersionColumnAndIgnoreNullValues() {
    // arrange
    Customer customer = new Customer();
    customer.setName("something");

    server.save(customer);

    // act
    Customer customerWithoutChanges = new Customer();
    customerWithoutChanges.setId(customer.getId());
    server.update(customerWithoutChanges);

    Customer result = Ebean.find(Customer.class, customer.getId());

    // assert
    Assert.assertEquals(customer.getUpdtime().getTime(), result.getUpdtime().getTime());
  }

  /**
   * Many relations mustn't be deleted when they are not loaded.
   */
  @Test
  public void testStatelessUpdateIgnoreNullCollection() {

    // arrange
    Contact contact = new Contact();
    contact.setFirstName("wobu :P");

    Customer customer = new Customer();
    customer.setName("something");
    customer.setContacts(new ArrayList<Contact>());
    customer.getContacts().add(contact);

    server.save(customer);

    // act
    Customer customerWithChange = new Customer();
    customerWithChange.setId(customer.getId());
    customerWithChange.setName("new name");

    // contacts is not loaded
    Assert.assertFalse(containsContacts(customerWithChange));
    server.update(customerWithChange);

    Customer result = Ebean.find(Customer.class, customer.getId());

    // assert null list was ignored (missing children not deleted)
    Assert.assertNotNull(result.getContacts());
    Assert.assertFalse("the contacts mustn't be deleted", result.getContacts().isEmpty());
  }
  
  /**
   * When BeanCollection is inadvertantly initialised and empty then ignore it
   * Specifically a non-BeanCollection (like ArrayList) is not ignored in terms
   * of deleting missing children.
   */
  @Test
  public void testStatelessUpdateIgnoreEmptyBeanCollection() {

    // arrange
    Contact contact = new Contact();
    contact.setFirstName("wobu :P");

    Customer customer = new Customer();
    customer.setName("something");
    customer.setContacts(new ArrayList<Contact>());
    customer.getContacts().add(contact);

    server.save(customer);

    // act
    Customer customerWithChange = new Customer();
    customerWithChange.setId(customer.getId());
    customerWithChange.setName("new name");

    // with Ebean enhancement this loads the an empty contacts BeanList
    customerWithChange.getContacts();
    
    // contacts has been initialised to empty BeanList
    Assert.assertTrue(containsContacts(customerWithChange));
    server.update(customerWithChange);

    Customer result = Ebean.find(Customer.class, customer.getId());

    // assert empty bean list was ignore (missing children not deleted)
    Assert.assertNotNull(result.getContacts());
    Assert.assertFalse("the contacts mustn't be deleted", result.getContacts().isEmpty());
  }
  
  @Test
  public void testStatelessUpdateDeleteChildrenForNonBeanCollection() {

    // arrange
    Contact contact = new Contact();
    contact.setFirstName("wobu :P");

    Customer customer = new Customer();
    customer.setName("something");
    customer.setContacts(new ArrayList<Contact>());
    customer.getContacts().add(contact);

    server.save(customer);

    // act
    Customer customerWithChange = new Customer();
    customerWithChange.setId(customer.getId());
    customerWithChange.setName("new name");

    // with Ebean enhancement this loads the an empty contacts BeanList
    customerWithChange.setContacts(Collections.<Contact> emptyList());
    
    Assert.assertTrue(containsContacts(customerWithChange));
    server.update(customerWithChange);

    Customer result = Ebean.find(Customer.class, customer.getId());

    // assert empty bean list was ignore (missing children not deleted)
    Assert.assertNotNull(result.getContacts());
    Assert.assertTrue("the contacts were deleted", result.getContacts().isEmpty());
  }
  
  private boolean containsContacts(Customer cust) {
    return server.getBeanState(cust).getLoadedProps().contains("contacts");
  }
  
}
