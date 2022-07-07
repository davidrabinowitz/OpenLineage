# SPDX-License-Identifier: Apache-2.0.
from typing import List, TYPE_CHECKING
from openlineage.airflow.extractors.postgres_extractor import PostgresExtractor
# from openlineage.airflow.utils import safe_import_airflow

if TYPE_CHECKING:
    from airflow.models import BaseHook


class RedshiftSQLExtractor(PostgresExtractor):
    default_schema = 'public'

    @classmethod
    def get_operator_classnames(cls) -> List[str]:
        return ['RedshiftSQLOperator']

    def _get_scheme(self) -> str:
        return 'redshift'

    def _get_hook(self) -> "BaseHook":
        from airflow.providers.amazon.aws.hooks.redshift_sql import RedshiftSQLHook
        return RedshiftSQLHook(
            redshift_conn_id=self.operator.redshift_conn_id,
        )
