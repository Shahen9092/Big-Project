package org.example.pages

fun pageCss(): String {
    return """
        <style>
            body {
                margin: 0;
                font-family: Arial, sans-serif;
                background: linear-gradient(135deg, #edf5ee, #ffffff);
                color: #173b17;
            }

            main {
                max-width: 1100px;
                margin: 40px auto;
                padding: 20px;
            }

            .box {
                background: white;
                padding: 30px;
                border-radius: 18px;
                box-shadow: 0 8px 24px rgba(0,0,0,0.08);
                border-top: 7px solid #2e7d32;
            }

            .top-logo {
                text-align: center;
                margin-bottom: 10px;
            }

            .top-logo img {
                width: 190px;
                height: auto;
            }

            .nav {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 25px;
                padding-bottom: 15px;
                border-bottom: 1px solid #dce9dd;
            }

            .nav a {
                text-decoration: none;
                color: #2e7d32;
                font-weight: bold;
            }

            h1 {
                color: #1b5e20;
                margin-top: 0;
                margin-bottom: 10px;
                font-size: 40px;
            }

            h2 {
                color: #1b5e20;
            }

            h3 {
                color: #1b5e20;
                margin-top: 0;
                margin-bottom: 10px;
            }

            p {
                line-height: 1.6;
                margin-top: 0;
            }

            .hero {
                font-size: 18px;
                color: #355b36;
                margin-bottom: 25px;
            }

            .action-grid {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 16px;
                align-items: start;
                margin-bottom: 20px;
            }

            .stats-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 16px;
                align-items: start;
                margin-bottom: 20px;
            }

            .info-grid {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 16px;
                align-items: start;
                margin-top: 10px;
            }

            .calendar-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 16px;
                align-items: start;
            }

            .month-switcher {
                display: grid;
                grid-template-columns: 80px 1fr 80px;
                align-items: center;
                gap: 15px;
                margin: 28px 0 24px 0;
            }

            .month-arrow-left {
                text-align: left;
            }

            .month-arrow-right {
                text-align: right;
            }

            .month-arrow {
                min-width: 52px;
                text-align: center;
            }

            .month-title-centre {
                background: #e8f5e9;
                border-left: 6px solid #2e7d32;
                border-radius: 12px;
                padding: 14px 18px;
                text-align: center;
            }

            .month-title-centre h2 {
                margin: 0;
                color: #1b5e20;
            }

            .month-section {
                margin-top: 35px;
                padding-top: 20px;
                border-top: 2px solid #dce9dd;
            }

            .month-title {
                background: #e8f5e9;
                border-left: 6px solid #2e7d32;
                padding: 12px 16px;
                border-radius: 12px;
                margin-bottom: 18px;
            }

            .action-card {
                background: #f4fbf4;
                border: 1px solid #cfe3d0;
                border-left: 6px solid #2e7d32;
                border-radius: 16px;
                padding: 20px;
            }

            .mini-card {
                background: #f7fbf7;
                border: 1px solid #d6e8d7;
                border-radius: 16px;
                padding: 20px;
            }

            .calendar-card {
                background: #f7fbf7;
                border: 1px solid #d6e8d7;
                border-radius: 16px;
                padding: 20px;
            }

            .calendar-entry {
                border-top: 1px solid #dfeadf;
                padding-top: 12px;
                margin-top: 12px;
            }

            .calendar-entry:first-of-type {
                border-top: none;
                padding-top: 0;
                margin-top: 0;
            }

            .big-stat {
                font-size: 38px;
                font-weight: bold;
                color: #1b5e20;
                margin: 8px 0 6px 0;
            }

            .small-stat {
                font-size: 28px;
            }

            .muted {
                color: #5e7560;
            }

            .small-note {
                font-size: 14px;
            }

            .record-list,
            .simple-list,
            .activity-set-list {
                margin: 0;
                padding-left: 20px;
            }

            .record-list li,
            .simple-list li,
            .activity-set-list li {
                margin-bottom: 8px;
            }

            input, select {
                padding: 13px;
                width: 95%;
                margin-top: 6px;
                border: 2px solid #c8e6c9;
                border-radius: 10px;
                font-size: 16px;
                background: white;
            }

            input:focus, select:focus {
                outline: none;
                border-color: #2e7d32;
                box-shadow: 0 0 0 3px rgba(46,125,50,0.15);
            }

            label {
                font-weight: bold;
                color: #1b5e20;
            }

            button, .btn {
                background: #2e7d32;
                color: white;
                border: none;
                padding: 12px 20px;
                border-radius: 10px;
                text-decoration: none;
                cursor: pointer;
                display: inline-block;
                font-weight: bold;
                transition: 0.2s;
            }

            button:hover, .btn:hover {
                background: #1f5a24;
            }

            .btn-light {
                background: white;
                color: #2e7d32;
                border: 2px solid #2e7d32;
                padding: 10px 18px;
                border-radius: 10px;
                text-decoration: none;
                display: inline-block;
            }

            .btn-light:hover {
                background: #eaf6ea;
            }

            .btn-danger {
                background: #b00020;
                color: white;
                border: none;
                padding: 12px 20px;
                border-radius: 10px;
                text-decoration: none;
                cursor: pointer;
                display: inline-block;
                font-weight: bold;
                margin-top: 10px;
            }

            .btn-danger:hover {
                background: #7f0016;
            }

            .btn-small {
                padding: 8px 14px;
                font-size: 14px;
                margin-top: 4px;
            }

            .tiny-actions {
                display: flex;
                gap: 8px;
                align-items: center;
                margin-top: 10px;
                flex-wrap: wrap;
            }

            .tiny-actions form {
                margin: 0;
            }

            .tiny-actions .btn-danger {
                margin-top: 0;
            }

            .error {
                background: #ffecec;
                color: #b00020;
                padding: 12px;
                border-radius: 10px;
                border-left: 5px solid #b00020;
                margin-bottom: 15px;
            }

            .success {
                background: #e8f5e9;
                color: #1b5e20;
                padding: 12px;
                border-radius: 10px;
                border-left: 5px solid #2e7d32;
                font-weight: bold;
                margin-bottom: 15px;
            }

            .home-box {
                text-align: center;
                max-width: 520px;
                margin: 0 auto;
                padding-top: 30px;
                padding-bottom: 40px;
            }

            .home-text {
                font-size: 18px;
                margin-bottom: 30px;
            }

            .home-buttons {
                display: flex;
                flex-direction: column;
                gap: 15px;
                align-items: center;
            }

            .big-btn {
                width: 280px;
                padding: 18px 0;
                font-size: 20px;
            }

            .search-form {
                background: #f4fbf4;
                padding: 18px;
                border-radius: 14px;
                border: 1px solid #c8e6c9;
                margin-bottom: 25px;
            }

            .search-row {
                display: grid;
                grid-template-columns: 1fr 220px 130px;
                gap: 12px;
                align-items: end;
            }

            .exercise-list {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 15px;
                margin-top: 20px;
                align-items: start;
            }

            .exercise-card {
                background: #f4fbf4;
                border: 1px solid #c8e6c9;
                border-radius: 14px;
                padding: 18px;
            }

            .exercise-card h3 {
                margin-top: 0;
                color: #1b5e20;
            }

            .exercise-card-top {
                display: flex;
                justify-content: space-between;
                gap: 12px;
                align-items: flex-start;
                margin-bottom: 14px;
            }

            .choose-btn {
                margin-top: 8px;
            }

            .unit-tag {
                display: inline-block;
                background: white;
                color: #2e7d32;
                border: 1px solid #2e7d32;
                border-radius: 999px;
                padding: 5px 10px;
                font-size: 13px;
                font-weight: bold;
            }

            .set-row {
                background: #f4fbf4;
                border: 1px solid #c8e6c9;
                border-radius: 14px;
                padding: 15px;
                margin-bottom: 12px;
            }

            .amount-line {
                display: flex;
                align-items: center;
                gap: 12px;
            }

            .amount-line input {
                width: 180px;
            }

            .amount-line span {
                color: #1b5e20;
                font-weight: bold;
                font-size: 18px;
            }

            .graph-form {
                background: #f4fbf4;
                border: 1px solid #d6e8d7;
                border-radius: 16px;
                padding: 20px;
                margin-bottom: 20px;
            }

            .graph-form-row {
                display: grid;
                grid-template-columns: 1fr 150px;
                gap: 12px;
                align-items: end;
            }

            .graph-box {
                background: #f7fbf7;
                border: 1px solid #d6e8d7;
                border-radius: 16px;
                padding: 20px;
            }

            .graph-wrap {
                width: 100%;
                overflow-x: auto;
                margin-top: 15px;
                margin-bottom: 20px;
            }

            .graph-svg {
                width: 100%;
                min-width: 760px;
                height: auto;
                background: white;
                border: 1px solid #dce9dd;
                border-radius: 12px;
            }

            .progress-table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 10px;
            }

            .progress-table th,
            .progress-table td {
                text-align: left;
                padding: 10px;
                border-bottom: 1px solid #dce9dd;
            }

            .progress-table th {
                color: #1b5e20;
            }

            .friends-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 16px;
                align-items: start;
                margin-top: 20px;
            }

            .friend-row {
                border-top: 1px solid #dfeadf;
                padding-top: 12px;
                margin-top: 12px;
                display: flex;
                justify-content: space-between;
                gap: 12px;
                align-items: center;
            }

            .friend-row:first-of-type {
                border-top: none;
                margin-top: 0;
                padding-top: 0;
            }

            .friend-row p {
                margin-bottom: 0;
            }

            .friend-actions {
                display: flex;
                gap: 8px;
                flex-wrap: wrap;
            }

            .friend-actions form {
                margin: 0;
            }

            .activity-search-card {
                background: linear-gradient(135deg, #f4fbf4, #ffffff);
                border: 1px solid #cfe3d0;
                border-left: 6px solid #2e7d32;
                border-radius: 18px;
                padding: 22px;
                margin-bottom: 28px;
                box-shadow: 0 4px 14px rgba(0,0,0,0.04);
            }

            .filter-top {
                display: flex;
                justify-content: space-between;
                gap: 16px;
                align-items: flex-start;
                margin-bottom: 18px;
            }

            .filter-top h3 {
                margin-bottom: 4px;
            }

            .results-count {
                background: white;
                border: 1px solid #c8e6c9;
                color: #1b5e20;
                padding: 8px 12px;
                border-radius: 999px;
                font-weight: bold;
                white-space: nowrap;
            }

            .activity-search-grid {
                display: grid;
                grid-template-columns: 1.6fr 0.9fr auto;
                gap: 14px;
                align-items: end;
            }

            .input-with-icon {
                position: relative;
            }

            .input-with-icon span {
                position: absolute;
                left: 14px;
                top: 50%;
                transform: translateY(-45%);
                color: #2e7d32;
                font-weight: bold;
                font-size: 20px;
            }

            .input-with-icon input {
                width: 100%;
                box-sizing: border-box;
                padding-left: 42px;
            }

            .category-select {
                width: 100%;
                box-sizing: border-box;
            }

            .filter-buttons {
                display: flex;
                gap: 10px;
                align-items: center;
            }

            .clear-filter {
                padding: 10px 16px;
            }

            .checkbox-list {
                background: white;
                border: 1px solid #d6e8d7;
                border-radius: 14px;
                padding: 14px;
                margin-top: 10px;
                max-height: 300px;
                overflow-y: auto;
            }

            .checkbox-row {
                display: flex;
                gap: 10px;
                align-items: center;
                padding: 8px;
                border-bottom: 1px solid #edf5ee;
                font-weight: normal;
            }

            .checkbox-row:last-child {
                border-bottom: none;
            }

            .checkbox-row input {
                width: auto;
                margin: 0;
            }            


            
        </style>
    """.trimIndent()
}

//alot of the front end utilised AI as we all lacked experienced with the language