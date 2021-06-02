import {cleanProjectsData, createProjectsData, login} from "../../common/apiCalls";
import {HOST} from "../../common/constants";
import 'cypress-file-upload';
import {assertMessage, gcy} from "../../common/shared";

describe('Projects Basics', () => {
    beforeEach(() => {
        cleanProjectsData()
        createProjectsData()
        login("cukrberg@facebook.com", "admin")
        cy.visit(`${HOST}`)
    })

    it("Searches in list", () => {
        gcy("global-list-search").find("input").type("Facebook")
        gcy("global-paginated-list").within(() => gcy("global-list-item").should("have.length", 1)).contains("Facebook itself")
    })

    it("Creates project with user owner", () => {
        createProject("I am a great project", "Mark Cukrberg")
    })

    it("Creates with organization owner", () => {
        createProject("I am a great project", "Facebook")
    })

    const createProject = (name: string, owner: string) => {
        gcy("global-plus-button").click()
        gcy("project-owner-select").click()
        gcy("project-owner-select-item").contains(owner).click()
        gcy("project-name-field").find("input").type(name)
        gcy("project-language-name-field").find("input").type("English")
        gcy("project-language-abbreviation-field").find("input").type("en")
        gcy("global-field-array-plus-button").click()
        gcy("project-language-name-field").eq(1).find("input").type("Deutsch")
        gcy("project-language-abbreviation-field").eq(1).find("input").type("de")
        gcy("global-field-array-plus-button").click()
        gcy("project-language-name-field").eq(2).find("input").type("Česky")
        gcy("project-language-abbreviation-field").eq(2).find("input").type("cs")
        gcy("global-form-save-button").click()
        assertMessage("Project created")
        gcy("global-paginated-list").contains(name).closest("li").within(() => {
            gcy("project-list-owner").contains(owner).should("be.visible")
        })
    }

    after(() => {
    })
})
