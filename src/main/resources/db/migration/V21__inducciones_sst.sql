CREATE TABLE apoyo.sst_material (
    id UUID PRIMARY KEY,
    obra_id BIGINT REFERENCES apoyo.obras_ingenieros(id),
    titulo VARCHAR(150) NOT NULL
);
CREATE TABLE apoyo.sst_version (
    material_id UUID NOT NULL REFERENCES apoyo.sst_material(id),
    version INTEGER NOT NULL,
    ejemplo BOOLEAN NOT NULL,
    contenido TEXT NOT NULL,
    PRIMARY KEY (material_id, version)
);
CREATE TABLE apoyo.sst_asignacion (
    id UUID PRIMARY KEY,
    obra_id BIGINT NOT NULL REFERENCES apoyo.obras_ingenieros(id),
    material_id UUID NOT NULL,
    version INTEGER NOT NULL,
    creada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    cerrada_en TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (material_id, version) REFERENCES apoyo.sst_version(material_id, version)
);
CREATE INDEX sst_asignacion_obra_idx ON apoyo.sst_asignacion(obra_id, creada_en);
CREATE TABLE apoyo.sst_trabajador (
    id UUID PRIMARY KEY,
    asignacion_id UUID NOT NULL REFERENCES apoyo.sst_asignacion(id),
    nombre VARCHAR(150) NOT NULL,
    numero INTEGER NOT NULL,
    pin_hash VARCHAR(64) NOT NULL,
    intentos INTEGER NOT NULL DEFAULT 0,
    ventana_hasta TIMESTAMP WITH TIME ZONE,
    sesion_hash VARCHAR(64) UNIQUE,
    sesion_hasta TIMESTAMP WITH TIME ZONE,
    estado VARCHAR(24) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'POR_REVISAR', 'VALIDADA', 'CORRECCION')),
    UNIQUE (asignacion_id, numero)
);
CREATE TABLE apoyo.sst_pin_emitido (
    asignacion_id UUID NOT NULL REFERENCES apoyo.sst_asignacion(id),
    pin_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (asignacion_id, pin_hash)
);
CREATE TABLE apoyo.sst_respuesta (
    trabajador_id UUID NOT NULL REFERENCES apoyo.sst_trabajador(id),
    pregunta INTEGER NOT NULL,
    opcion INTEGER NOT NULL,
    correcta BOOLEAN NOT NULL,
    intentada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (trabajador_id, pregunta)
);
CREATE TABLE apoyo.sst_entrega (
    id UUID NOT NULL,
    trabajador_id UUID NOT NULL REFERENCES apoyo.sst_trabajador(id),
    foto BYTEA NOT NULL,
    enviada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    revisada_en TIMESTAMP WITH TIME ZONE,
    revisor_uid VARCHAR(255),
    decision VARCHAR(24) CHECK (decision IN ('VALIDADA', 'CORRECCION')),
    observacion VARCHAR(1000),
    PRIMARY KEY (trabajador_id, id)
);
CREATE INDEX sst_entrega_trabajador_idx ON apoyo.sst_entrega(trabajador_id, enviada_en DESC);

INSERT INTO apoyo.sst_material (id, titulo)
VALUES ('1eb48aaa-c389-4c3c-8379-a0e092fc830e', 'Trabajo en alturas');

INSERT INTO apoyo.sst_version VALUES ('1eb48aaa-c389-4c3c-8379-a0e092fc830e', 1, true, '{
  "secciones": [
    {"titulo":"Riesgos de la labor","texto":"El explorador SST identifica caídas al mismo nivel, caídas a desnivel y golpes por herramientas que caen. Repasa con el responsable SST los riesgos y controles específicos de la obra."},
    {"titulo":"EPP y evidencia","texto":"El explorador SST menciona casco, arnés, línea de vida y botas como referencias para esta labor. El responsable SST define qué equipo aplica en la obra. Toma la foto con el EPP puesto en un lugar seguro, antes de iniciar la tarea."},
    {"titulo":"Inicio de la tarea","texto":"Confirma por separado la autorización de inicio con el responsable de la obra."}
  ],
  "preguntas": [
    {"texto":"¿Con quién se revisan los riesgos específicos?","opciones":["Con el responsable SST","Con cualquier visitante"],"correcta":0,"seccion":0},
    {"texto":"¿Dónde se toma la foto?","opciones":["Durante la tarea","En un lugar seguro antes de iniciar"],"correcta":1,"seccion":1},
    {"texto":"¿Qué trámite se confirma por separado?","opciones":["La autorización de inicio","La lectura del material"],"correcta":0,"seccion":2}
  ]
}');
