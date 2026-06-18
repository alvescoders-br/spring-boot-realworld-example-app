alter table articles
  add column is_deleted boolean not null default false;

alter table articles
  drop constraint articles_slug_key;

create unique index articles_slug_active_unique
  on articles (slug)
  where is_deleted = false;
