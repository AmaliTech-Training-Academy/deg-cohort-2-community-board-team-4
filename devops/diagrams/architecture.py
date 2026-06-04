"""
CommunityBoard — AWS infrastructure & CI/CD architecture (official AWS icons).

Renders an "AWS standard" chart with the mingrammer `diagrams` library
(https://diagrams.mingrammer.com/), which uses the official AWS component icons.

Usage
-----
    # Requires Graphviz on the system:
    #   macOS:  brew install graphviz
    #   Debian: sudo apt-get install -y graphviz
    cd devops/diagrams
    python3 -m venv .venv && source .venv/bin/activate
    pip install -r requirements.txt
    python architecture.py
    # -> communityboard-aws-architecture.png

This mirrors the deployed topology described in ARCHITECTURE.md / DEPLOYMENT.md:
Terraform provisions an EC2 host in the default VPC; the host runs the full stack
via Docker Compose pulling images from Docker Hub; GitHub Actions builds/scans/
pushes images (CI) and deploys over SSH (CD); state lives in S3; staging logs go
to CloudWatch Logs.
"""

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import EC2
from diagrams.aws.database import RDSPostgresqlInstance  # used here as the PostgreSQL icon
from diagrams.aws.devtools import Codepipeline
from diagrams.aws.management import Cloudwatch
from diagrams.aws.network import VPC, InternetGateway
from diagrams.aws.security import IdentityAndAccessManagementIamPermissions as KeyPair
from diagrams.aws.storage import S3
from diagrams.onprem.client import Users
from diagrams.onprem.container import Docker
from diagrams.onprem.registry import Harbor as DockerHub  # registry icon stand-in
from diagrams.onprem.vcs import Github

GRAPH_ATTR = {"fontsize": "20", "labelloc": "t", "pad": "0.6", "splines": "spline"}

with Diagram(
    "CommunityBoard — AWS Architecture & CI/CD",
    filename="communityboard-aws-architecture",
    show=False,
    direction="LR",
    graph_attr=GRAPH_ATTR,
):
    users = Users("End users")
    devs = Users("Developers")

    # ---- CI/CD plane (GitHub Actions) ----------------------------------------
    with Cluster("GitHub Actions (CI/CD)"):
        gh = Github("repo: dev/test/main")
        pipeline = Codepipeline("CI Pipeline\nbuild · test · scan")
        cd = Codepipeline("CD\nscp compose + ssh")
        gh >> pipeline >> cd

    registry = DockerHub("Docker Hub\ncommunityboard-* images")

    # ---- AWS cloud -----------------------------------------------------------
    with Cluster("AWS Cloud"):
        state = S3("Terraform state")
        logs = Cloudwatch("CloudWatch Logs\n(staging)")
        key = KeyPair("EC2 Key Pair")

        with Cluster("Default VPC"):
            igw = InternetGateway("IGW")
            vpc = VPC("Security Group\n22 / 3000 / 8080")

            with Cluster("EC2 — Amazon Linux 2 (t2.micro)\nDocker Compose"):
                host = EC2("App host")
                with Cluster("Containers"):
                    frontend = Docker("frontend :3000")
                    backend = Docker("backend :8080")
                    data_eng = Docker("data-engineering\nETL + seed")
                    db = RDSPostgresqlInstance("PostgreSQL :5432")

                    frontend >> Edge(label="REST") >> backend
                    backend >> Edge(label="JDBC") >> db
                    data_eng >> Edge(label="seed/ETL") >> db
                host >> [frontend, backend, data_eng, db]

    # ---- Wiring --------------------------------------------------------------
    users >> Edge(label="HTTP :3000/:8080") >> igw >> vpc >> host
    devs >> Edge(label="git push") >> gh

    pipeline >> Edge(label="push images") >> registry
    registry >> Edge(label="docker compose pull") >> host
    cd >> Edge(label="deploy (SSH)") >> host

    host >> Edge(style="dashed", label="logs") >> logs
    key >> Edge(style="dashed", label="provisions") >> host
    cd >> Edge(style="dotted") >> state
