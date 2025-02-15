import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { assertMessage, gcy } from '../../common/shared';
import { getAnyContainingText } from '../../common/xPath';
import { login } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';

describe('Organization Invitations', () => {
  beforeEach(() => {
    login();
    organizationTestData.clean();
    organizationTestData.generate();
    visit();
  });

  it('generates invitations', () => {
    generateInvitation('MEMBER');

    gcy('organization-invitations-generated-field').within(() => {
      cy.get('textarea').should('contain.value', 'http://');
    });

    generateInvitation('OWNER');
    generateInvitation('OWNER');
    generateInvitation('MEMBER');
    cy.xpath("//*[@data-cy='global-list-items']//li").should('have.length', 4);
    gcy('global-list-items')
      .xpath('.' + getAnyContainingText('MEMBER'))
      .should('have.length', 2);
    gcy('global-list-items')
      .xpath('.' + getAnyContainingText('OWNER'))
      .should('have.length', 2);
  });

  it('cancels invitation', () => {
    generateInvitation('MEMBER');
    generateInvitation('OWNER');

    cy.xpath("//*[@data-cy='global-list-items']//li").should('have.length', 2);
    gcy('organization-invitation-cancel-button').eq(0).click();
    cy.xpath("//*[@data-cy='global-list-items']//li").should('have.length', 1);
    gcy('organization-invitation-cancel-button').click();
    gcy('global-list-items').should('not.exist');
  });

  it('owner invitation can be accepted', () => {
    testAcceptInvitation('OWNER');
  });

  it('member invitation can be accepted', () => {
    testAcceptInvitation('MEMBER');
  });

  after(() => {
    organizationTestData.clean();
  });

  const visit = () => {
    cy.visit(`${HOST}/organizations/tolgee/invitations`);
  };

  const generateInvitation = (roleType: 'MEMBER' | 'OWNER') => {
    gcy('organization-invitation-role-select').click();
    gcy('organization-role-select-item')
      .filter(':visible')
      .within(() => {
        cy.contains(roleType).click();
      });
    gcy('organization-invitation-generate-button').click();
  };

  const testAcceptInvitation = (roleType: 'MEMBER' | 'OWNER') => {
    generateInvitation(roleType);

    gcy('organization-invitations-generated-field')
      .find('textarea')
      .invoke('val')
      .then((val) => {
        login('owner@zzzcool12.com', 'admin');
        cy.visit(val as string);
      });

    assertMessage('Invitation successfully accepted');
    cy.visit(`${HOST}/organizations`);
    cy.gcy('global-paginated-list')
      .contains('Tolgee')
      .should('be.visible')
      .closest('li')
      .within(() => {
        cy.gcy('organization-settings-button').should(
          roleType === 'MEMBER' ? 'not.exist' : 'be.visible'
        );
      });
  };
});
