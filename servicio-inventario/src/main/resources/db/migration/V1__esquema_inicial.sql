-- InventaYa - esquema inicial de la base compartida inventaya_db.
--
-- Este servicio (servicio-inventario) es el dueno del esquema: es el unico que ejecuta
-- Flyway. servicio-ventas arranca con flyway deshabilitado y ddl-auto=none, y solo
-- lee/escribe sobre estas mismas tablas.

create table productos (
    id            uuid          primary key,
    nombre        varchar(180)  not null,
    precio        numeric(14,2) not null check (precio >= 0),
    stock         integer       not null check (stock >= 0),
    umbral_minimo integer       not null check (umbral_minimo >= 0),
    creado_en     timestamp     not null default now()
);

create table ventas (
    id               uuid          primary key,
    total            numeric(14,2) not null,
    estado           varchar(20)   not null check (estado in ('REGISTRADA', 'ANULADA')),
    motivo_anulacion varchar(500),
    fecha            timestamp     not null default now()
);

create index idx_ventas_fecha on ventas (fecha);

create table venta_items (
    id              uuid          primary key,
    venta_id        uuid          not null references ventas (id),
    producto_id     uuid          not null references productos (id),
    cantidad        integer       not null check (cantidad > 0),
    precio_unitario numeric(14,2) not null check (precio_unitario >= 0),
    subtotal        numeric(14,2) not null
);

create index idx_venta_items_venta on venta_items (venta_id);

create table alertas_stock (
    id               uuid      primary key,
    producto_id      uuid      not null references productos (id),
    stock_al_momento integer   not null,
    umbral           integer   not null,
    fecha            timestamp not null default now(),
    atendida         boolean   not null default false
);

create index idx_alertas_stock_atendida on alertas_stock (atendida);

-- Soporte de idempotencia para los consumidores de Kafka: cada eventoId aplicado
-- queda registrado aqui, de modo que una reentrega del mismo evento no vuelve a
-- descontar ni a revertir stock.
create table eventos_procesados (
    evento_id    uuid        primary key,
    tipo         varchar(40) not null,
    procesado_en timestamp   not null default now()
);
