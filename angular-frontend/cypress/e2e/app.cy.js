describe('Dashboard', () => {
 
  beforeEach(() => {
    cy.intercept('GET', '**/merge', {
      statusCode: 200,
      body: [
        { merge_id: 'merge-test-001', config: '{}' },
        { merge_id: 'merge-test-002', config: '{}' }
      ]
    }).as('getMerges');
    cy.visit('/');
  });
 
  it('muestra el título CalendarFusion', () => {
    cy.contains('CalendarFusion').should('be.visible');
  });
 
  it('carga y muestra la lista de fusiones', () => {
    cy.wait('@getMerges');
    cy.contains('merge-test-001').should('be.visible');
    cy.contains('merge-test-002').should('be.visible');
  });
 
  it('muestra botones de Copiar y Eliminar por cada fusión', () => {
    cy.wait('@getMerges');
    cy.contains('merge-test-001').should('be.visible');
    cy.contains('Copiar').should('exist');
    cy.contains('Eliminar').should('exist');
  });
 
  it('elimina una fusión al pulsar Eliminar', () => {
    cy.intercept('DELETE', '**/merge/merge-test-001', {
      statusCode: 200,
      body: { deleted: 'merge-test-001' }
    }).as('deleteMerge');
 
    cy.wait('@getMerges');
    cy.contains('Eliminar').first().click();
    cy.wait('@deleteMerge');
  });
 
  it('navega a Nueva fusión al pulsar el botón', () => {
    cy.contains('Nueva fusión').click();
    cy.url().should('include', '/merge/new');
  });
 
  it('navega a Gestionar cuentas al pulsar el botón', () => {
    cy.contains('Gestionar cuentas').click();
    cy.url().should('include', '/accounts');
  });
 
  it('muestra estado vacío si no hay fusiones', () => {
    cy.intercept('GET', '**/merge', { statusCode: 200, body: [] }).as('getMergesVacio');
    cy.visit('/');
    cy.wait('@getMergesVacio');
    cy.get('body').should('not.contain', 'merge-test-001');
  });
});
 
describe('Cuentas', () => {
 
  it('muestra las tarjetas de Google y CalDAV', () => {
    cy.visit('/accounts');
    cy.contains('Google Calendar').should('be.visible');
    cy.contains('CalDAV').should('be.visible');
  });
 
  it('muestra botón Conectar con Google', () => {
    cy.visit('/accounts');
    cy.contains('Conectar con Google').should('be.visible');
  });
 
  it('muestra mensaje de cuenta conectada si viene el parámetro connected', () => {
    cy.visit('/accounts?connected=google-123456');
    cy.contains('google-123456').should('be.visible');
  });
 
  it('navega al formulario CalDAV al pulsar Conectar CalDAV', () => {
    cy.visit('/accounts');
    cy.contains('Conectar CalDAV').click();
    cy.url().should('include', '/accounts/caldav');
  });
});
 
describe('Conectar CalDAV', () => {
 
  it('muestra el formulario con los campos necesarios', () => {
    cy.visit('/accounts/caldav');
    cy.get('input').should('have.length.greaterThan', 2);
  });
 
  it('conecta correctamente y redirige a accounts', () => {
    cy.intercept('POST', '**/caldav/auth/login', {
      statusCode: 200,
      body: 'Sesión CalDAV iniciada para: test-account'
    }).as('caldavLogin');
 
    cy.visit('/accounts/caldav');
    cy.get('input').eq(0).type('test-account');
    cy.get('input').eq(1).type('https://caldav.icloud.com');
    cy.get('input').eq(2).type('usuario@gmail.com');
    cy.get('input').eq(3).type('contraseña-test');
    cy.get('button[type="submit"], button').last().click();
 
    cy.wait('@caldavLogin');
  });
 
  it('muestra error si las credenciales son incorrectas', () => {
    cy.intercept('POST', '**/caldav/auth/login', {
      statusCode: 401,
      body: 'Credenciales incorrectas'
    }).as('caldavLoginError');
 
    cy.visit('/accounts/caldav');
    cy.get('input').eq(0).type('cuenta-mala');
    cy.get('input').eq(1).type('https://caldav.icloud.com');
    cy.get('input').eq(2).type('malo@gmail.com');
    cy.get('input').eq(3).type('contraseña-mala');
    cy.get('button[type="submit"], button').last().click();
 
    cy.wait('@caldavLoginError');
  });
});
 
describe('Nueva fusión', () => {
 
  it('muestra el formulario de nueva fusión', () => {
    cy.visit('/merge/new');
    cy.contains('Nueva fusión').should('be.visible');
  });
 
  it('crea una fusión y muestra la URL ICS', () => {
    cy.intercept('POST', '**/merge', {
      statusCode: 200,
      body: { merge_id: 'nuevo-merge-123', ics_url: '/merge/nuevo-merge-123/ics' }
    }).as('createMerge');
 
    cy.intercept('GET', '**/google-calendar/**/calendars', {
      statusCode: 200,
      body: [{ id: 'primary', summary: 'Mi calendario' }]
    }).as('getCalendars');
 
    cy.visit('/merge/new');
    cy.contains('Añadir calendario fuente').click();
    cy.get('select').first().select('google');
    cy.get('input').first().type('google-123');
    cy.contains('Cargar').click();
    cy.wait('@getCalendars');
    cy.contains('Crear fusión').click();
    cy.wait('@createMerge');
    cy.contains('nuevo-merge-123').should('be.visible');
  });
 
  it('muestra error si no se añade ninguna fuente', () => {
    cy.visit('/merge/new');
    cy.contains('Crear fusión').click();
    cy.get('body').should('be.visible');
  });
});